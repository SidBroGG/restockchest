package com.sidbro.restockchest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sidbro.restockchest.RestockChest;
import com.sidbro.restockchest.data.RestockChestData;
import com.sidbro.restockchest.data.RestockChestEntry;
import com.sidbro.restockchest.logic.RestockChestMarkerService;
import com.sidbro.restockchest.logic.RestockChestService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = RestockChest.MODID)
public final class RestockChestCommand {
    private RestockChestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("restockchest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                                        .suggests(RestockChestCommand::suggestLootTables)
                                        .then(Commands.argument("time_seconds", LongArgumentType.longArg(1, Long.MAX_VALUE / 20L))
                                                .executes(RestockChestCommand::registerChest)
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                                                .suggests(RestockChestCommand::suggestLootTables)
                                                .then(Commands.argument("time_seconds", LongArgumentType.longArg(1, Long.MAX_VALUE / 20L))
                                                        .executes(RestockChestCommand::registerChestInRadius)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .executes(RestockChestCommand::removeChest)
                        )
                        .then(Commands.literal("info")
                                .executes(RestockChestCommand::getChestInfo)
                        )
                        .then(Commands.literal("restock")
                                .executes(RestockChestCommand::restockChest)
                        )
                        .then(Commands.literal("all")
                                .executes(RestockChestCommand::toggleChestMarkers)
                        )
        );
    }

    private static int registerChestInRadius(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();

        var radius = IntegerArgumentType.getInteger(context, "radius");
        var lootTableId = ResourceLocationArgument.getId(context, "loot_table");
        var cooldownTicks = LongArgumentType.getLong(context, "time_seconds") * 20L;

        if (!source.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE).contains(lootTableId)) {
            source.sendFailure(Component.translatable("command.restockchest.error.loot_table_not_found", lootTableId));
            return 0;
        }

        var center = player.blockPosition();
        var data = RestockChestData.get(level);
        int registeredCount = 0;

        for (var targetPos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            var blockEntity = level.getBlockEntity(targetPos);
            if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
                continue;
            }

            var state = level.getBlockState(targetPos);
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                continue;
            }

            if (data.contains(level.dimension(), targetPos)) {
                continue;
            }

            var entry = RestockChestEntry.create(level.dimension(), targetPos.immutable(), lootTableId, cooldownTicks);
            data.add(entry);

            if (RestockChestService.restock(level, entry)) {
                data.update(entry.stopTimer());
                registeredCount++;
            } else {
                data.remove(level.dimension(), targetPos);
            }
        }

        var finalCount = registeredCount;
        source.sendSuccess(() -> Component.translatable("command.restockchest.success.registered_radius", finalCount, radius), false);

        return finalCount;
    }

    private static CompletableFuture<Suggestions> suggestLootTables(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var chestLootTables = context.getSource()
                .getServer()
                .reloadableRegistries()
                .getKeys(Registries.LOOT_TABLE)
                .stream().filter(id -> id.getPath().startsWith("chests/"))
                .sorted();

        return SharedSuggestionProvider.suggestResource(
                chestLootTables,
                builder
        );
    }

    private static BlockPos findTargetContainer(ServerPlayer player, ServerLevel level) {
        HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);

        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return null;
        }

        return pos;
    }

    private static int registerChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();

        var lootTableId = ResourceLocationArgument.getId(context, "loot_table");
        var cooldownSeconds = LongArgumentType.getLong(context, "time_seconds");

        var cooldownTicks = cooldownSeconds * 20L;

        if (!source.getServer()
                .reloadableRegistries()
                .getKeys(Registries.LOOT_TABLE)
                .contains(lootTableId)) {
            source.sendFailure(Component.translatable("command.restockchest.error.loot_table_not_found", lootTableId));
            return 0;
        }

        var targetPos = findTargetContainer(player, level);

        if (targetPos == null) {
            source.sendFailure(Component.translatable("command.restockchest.error.container_not_found"));
            return 0;
        }

        var data = RestockChestData.get(level);

        if (data.contains(level.dimension(), targetPos)) {
            source.sendFailure(Component.translatable("command.restockchest.error.already_registered"));
            return 0;
        }

        var entry = RestockChestEntry.create(level.dimension(), targetPos, lootTableId, cooldownTicks);

        data.add(entry);

        var restocked = RestockChestService.restock(level, entry);

        if (!restocked) {
            source.sendFailure(Component.translatable("command.restockchest.error.restock_failed"));
            return 0;
        }

        data.update(entry.stopTimer());

        source.sendSuccess(() -> Component.translatable("command.restockchest.success.registered", targetPos.toShortString(), cooldownSeconds), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int removeChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();

        var targetPos = findTargetContainer(player, level);

        if (targetPos == null) {
            source.sendFailure(Component.translatable("command.restockchest.error.container_not_found"));
            return 0;
        }

        var data = RestockChestData.get(level);

        if (!data.contains(level.dimension(), targetPos)) {
            source.sendFailure(Component.translatable("command.restockchest.error.not_registered"));
            return 0;
        }

        data.remove(level.dimension(), targetPos);

        source.sendSuccess(() -> Component.translatable("command.restockchest.success.removed", targetPos.toShortString()), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int getChestInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();

        var targetPos = findTargetContainer(player, level);

        if (targetPos == null) {
            source.sendFailure(Component.translatable("command.restockchest.error.container_not_found"));
            return 0;
        }

        var data = RestockChestData.get(level);

        if (!data.contains(level.dimension(), targetPos)) {
            source.sendFailure(Component.translatable("command.restockchest.error.not_registered"));
            return 0;
        }

        var entry = data.get(level.dimension(), targetPos);

        if (entry.isActive()) {
            source.sendSuccess(() -> Component.translatable("command.restockchest.success.info_active", entry.lootTable().toString(), entry.cooldownTicks()), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.restockchest.success.info_disabled", entry.lootTable().toString(), entry.cooldownTicks()), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int restockChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var level = source.getLevel();

        var targetPos = findTargetContainer(player, level);

        if (targetPos == null) {
            source.sendFailure(Component.translatable("command.restockchest.error.container_not_found"));
            return 0;
        }

        var data = RestockChestData.get(level);

        if (!data.contains(level.dimension(), targetPos)) {
            source.sendFailure(Component.translatable("command.restockchest.error.not_registered"));
            return 0;
        }

        var entry = data.get(level.dimension(), targetPos);

        var restocked = RestockChestService.restock(level, entry);

        if (!restocked) {
            source.sendFailure(Component.translatable("command.restockchest.error.restock_failed"));
            return 0;
        }

        data.update(entry.stopTimer());

        source.sendSuccess(() -> Component.translatable("command.restockchest.success.restocked"), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int toggleChestMarkers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();

        var enabled = RestockChestMarkerService.toggle(source.getLevel(), player);

        if (enabled) {
            source.sendSuccess(() -> Component.translatable("command.restockchest.success.markers_enabled"), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.translatable("command.restockchest.success.markers_disabled"), false);
        return Command.SINGLE_SUCCESS;
    }
}

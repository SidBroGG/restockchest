package com.sidbro.restockchest.logic;

import com.sidbro.restockchest.data.RestockChestEntry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class RestockChestService {
    private RestockChestService() {
    }

    public static boolean restock(ServerLevel level, RestockChestEntry entry) {
        var blockEntity = level.getBlockEntity(entry.pos());

        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
            return false;
        }

        boolean lootTableExists = level.getServer()
                .reloadableRegistries()
                .getKeys(Registries.LOOT_TABLE)
                .stream().anyMatch(entry.lootTable()::equals);

        if (!lootTableExists) {
            return false;
        }

        var params = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(entry.pos())
                )
//                .withOptionalParameter(
//                        LootContextParams.THIS_ENTITY,
//                        player
//                )
//                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);

        var lootTable = level.getServer()
                .reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, entry.lootTable()));

        container.setLootTable(null);
        container.setLootTableSeed(0L);
        container.clearContent();

        lootTable.fill(
                container,
                params,
                level.random.nextLong()
        );

        container.setChanged();

        var state = level.getBlockState(entry.pos());

        level.sendBlockUpdated(entry.pos(), state, state, Block.UPDATE_CLIENTS);

        return true;
    }

    public static BlockEntity getBlockEntity(RestockChestEntry entry, ServerLevel level) {
        var pos = entry.pos();
        var blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
            return null;
        }

        var blockState = blockEntity.getBlockState();

        if (blockState.getBlock() instanceof ChestBlock && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return null;
        }

        return blockEntity;
    }

    public static void setTimeLeftName(RestockChestEntry entry, ServerLevel level) {
        var blockEntity = getBlockEntity(entry, level);
        var gameTime = level.getGameTime();

        if (blockEntity == null) {
            return;
        }

        if (!(blockEntity instanceof BaseContainerBlockEntity container)) {
            return;
        }

        var newComponents = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.translatable("chest.restockchest.time_left", (entry.nextRestockTicks() - gameTime) / 20L))
                .build();

        container.applyComponents(container.components(), newComponents);

        container.setChanged();

        var state = level.getBlockState(entry.pos());

        level.sendBlockUpdated(entry.pos(), state, state, Block.UPDATE_CLIENTS);
    }
}

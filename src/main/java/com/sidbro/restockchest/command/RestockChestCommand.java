package com.sidbro.restockchest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sidbro.restockchest.RestockChest;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RestockChest.MODID)
public final class RestockChestCommand {
    private RestockChestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("restockchest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                                .then(Commands.argument("time", LongArgumentType.longArg(1))
                                        .executes(context -> Command.SINGLE_SUCCESS)
                                )
                        )
        );
    }
}

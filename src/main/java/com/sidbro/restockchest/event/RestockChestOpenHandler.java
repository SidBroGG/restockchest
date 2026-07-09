package com.sidbro.restockchest.event;

import com.sidbro.restockchest.RestockChest;
import com.sidbro.restockchest.data.RestockChestData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

@EventBusSubscriber(modid = RestockChest.MODID)
public class RestockChestOpenHandler {
    private RestockChestOpenHandler() {
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getContainer() instanceof ChestMenu chestMenu)) {
            return;
        }

        if (!(chestMenu.getContainer() instanceof RandomizableContainerBlockEntity container)) {
            return;
        }

        var level = player.serverLevel();
        var pos = container.getBlockPos();

        var data = RestockChestData.get(level);

        if (!(data.contains(level.dimension(), pos))) {
            return;
        }

        var entry = data.get(level.dimension(), pos);

        if (entry.isActive()) {
            return;
        }

        data.update(entry.startTimer(level.getGameTime()));
    }
}

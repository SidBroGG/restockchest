package com.sidbro.restockchest.event;

import com.sidbro.restockchest.RestockChest;
import com.sidbro.restockchest.data.RestockChestData;
import com.sidbro.restockchest.logic.RestockChestService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RestockChest.MODID)
public class RestockChestUpdateHandler {
    private static final long TICKS_TO_UPDATE = 20L;

    private RestockChestUpdateHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var level = server.getLevel(server.overworld().dimension());

        assert level != null;
        var gameTime = level.getGameTime();

        if (gameTime % TICKS_TO_UPDATE != 0L) {
            return;
        }

        var data = RestockChestData.get(server.overworld());

        var containerEntries = data.all();

        for (var entry : containerEntries) {
            var blockEntity = RestockChestService.getBlockEntity(entry, level);

            if (blockEntity == null) {
                data.remove(level.dimension(), entry.pos());
                continue;
            }

            if (!entry.isActive()) {
                continue;
            }

            if (gameTime >= entry.nextRestockTicks()) {
                RestockChestService.restock(level, entry);
                data.update(entry.stopTimer());
                continue;
            }

            RestockChestService.setTimeLeftName(entry, level);
        }
    }


}

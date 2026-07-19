package com.sidbro.restockchest.event;

import com.sidbro.restockchest.RestockChest;
import com.sidbro.restockchest.data.RestockChestData;
import com.sidbro.restockchest.logic.RestockChestMarkerService;
import com.sidbro.restockchest.logic.RestockChestService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
            if (!level.isLoaded(entry.pos())) {
                continue;
            }

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
        }

        for (var player : server.getPlayerList().getPlayers()) {
            RestockChestMarkerService.refreshMarkers(player.serverLevel(), player);

            var hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);

            if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
                var playerLevel = player.serverLevel();
                var entry = data.get(playerLevel.dimension(), blockHit.getBlockPos());

                if (entry != null && entry.isActive()) {
                    var secondsLeft = Math.max(0L, (entry.nextRestockTicks() - playerLevel.getGameTime()) / 20L);
                    player.displayClientMessage(Component.translatable("chest.restockchest.time_left", secondsLeft), true);
                }
            }
        }
    }


}

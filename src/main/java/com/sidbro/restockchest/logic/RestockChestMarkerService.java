package com.sidbro.restockchest.logic;

import com.mojang.math.Transformation;
import com.sidbro.restockchest.data.RestockChestData;
import com.sidbro.restockchest.data.RestockChestEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class RestockChestMarkerService {
    private static final String MARKED_TAG = "restockchest_marker";
    private static final String OWNER_TAG_PREFIX = "restockchest_owner_";

    private static final int RADIUS_CHUNKS = 8;
    private static final float BEAM_WIDTH = 0.18F;

    private static final Set<String> ENABLED_PLAYER = new HashSet<>();

    private RestockChestMarkerService() {
    }

    public static boolean toggle(ServerLevel level, ServerPlayer player) {
        var playerName = player.getName().getString();

        if (ENABLED_PLAYER.remove(playerName)) {
            removeMarkers(level, player);
            return false;
        }

        ENABLED_PLAYER.add(playerName);
        refreshMarkers(level, player);

        return true;
    }

    public static void refreshMarkers(ServerLevel level, ServerPlayer player) {
        if (!ENABLED_PLAYER.contains(player.getName().getString())) {
            return;
        }

        removeMarkers(level, player);
        createMarkers(level, player);
    }

    private static String getOwnerTag(ServerPlayer player) {
        return OWNER_TAG_PREFIX + player.getName().getString();
    }

    private static void removeMarkers(ServerLevel level, ServerPlayer player) {
        var ownedTag = getOwnerTag(player);
        var markers = new ArrayList<Display.BlockDisplay>();

        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Display.BlockDisplay display)) {
                continue;
            }

            if (!display.getTags().contains(MARKED_TAG)) {
                continue;
            }

            if (!display.getTags().contains(ownedTag)) {
                continue;
            }

            markers.add(display);
        }

        for (var marker : markers) {
            marker.discard();
        }

    }

    private static void createMarkers(ServerLevel level, ServerPlayer player) {
        var data = RestockChestData.get(level);
        var centerChunk = player.chunkPosition();
        var ownerTag = getOwnerTag(player);

        var createdMarkers = 0;

        for (int chunkX = centerChunk.x - RADIUS_CHUNKS; chunkX <= centerChunk.x + RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunk.z - RADIUS_CHUNKS; chunkZ <= centerChunk.z + RADIUS_CHUNKS; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ChestBlockEntity)) {
                        continue;
                    }

                    var pos = blockEntity.getBlockPos();

                    if (!shouldCreateMarker(level, pos)) {
                        continue;
                    }

                    var entry = data.get(level.dimension(), pos);
                    var markerState = getMarkerState(entry);

                    if (createMarker(level, pos, markerState, ownerTag)) {
                        createdMarkers++;
                    }
                }
            }
        }

    }

    private static boolean shouldCreateMarker(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock)) {
            return false;
        }

        return state.getValue(ChestBlock.TYPE) != ChestType.RIGHT;
    }

    private static BlockState getMarkerState(RestockChestEntry entry) {
        if (entry == null) {
            return Blocks.RED_STAINED_GLASS.defaultBlockState();
        }

        if (entry.isActive()) {
            return Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
        }

        return Blocks.GREEN_STAINED_GLASS.defaultBlockState();
    }

    private static boolean createMarker(ServerLevel level, BlockPos pos, BlockState markerState, String ownerTag) {
        var display = EntityType.BLOCK_DISPLAY.create(level);

        if (display == null) {
            return false;
        }

        var beamHeight = Math.max(1.0F, level.getMaxBuildHeight() - pos.getY() - 1.0F);

        var entityTag = new CompoundTag();

        display.saveWithoutId(entityTag);

        entityTag.put("block_state", NbtUtils.writeBlockState(markerState));
        entityTag.put("transformation", createTransformationTag(beamHeight));
        entityTag.put("brightness", createBrightnessTag());
        entityTag.putFloat("viewRange", 8.0F);
        entityTag.putFloat("width", 1.0F);
        entityTag.putFloat("height", beamHeight);
        entityTag.putFloat("shadow_radius", 0.0F);
        entityTag.putFloat("shadow_strength", 0.0F);

        display.load(entityTag);

        display.setPos(pos.getX() + 0.5D - BEAM_WIDTH / 2.0D, pos.getY() + 1.0D, pos.getZ() + 0.5D - BEAM_WIDTH / 2.0D);

        display.addTag(MARKED_TAG);
        display.addTag(ownerTag);

        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);

        return level.addFreshEntity(display);
    }

    private static Tag createTransformationTag(float beamHeight) {
        var transformation = new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf(),
                new Vector3f(BEAM_WIDTH, beamHeight, BEAM_WIDTH),
                new Quaternionf()
        );

        return Transformation.EXTENDED_CODEC
                .encodeStart(NbtOps.INSTANCE, transformation)
                .result()
                .orElseThrow(() -> new IllegalStateException("failed to encode chest marker transformation"));
    }

    private static Tag createBrightnessTag() {
        var brightnessTag = new CompoundTag();

        brightnessTag.putInt("block", 15);
        brightnessTag.putInt("sky", 15);

        return brightnessTag;
    }
}

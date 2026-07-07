package com.sidbro.refillchest.data;

import com.jcraft.jorbis.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RestockChestData extends SavedData {
    private static final String DATA_NAME = "refillchests_restock_chests";

    private final Map<String, RestockChestEntry> chests = new HashMap<>();

    private static SavedData.Factory<RestockChestData> factory() {
        return new SavedData.Factory<>(
                RestockChestData::new,
                RestockChestData::load
        );
    }

    public static RestockChestData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public static RestockChestData load(CompoundTag tag, HolderLookup.Provider registries) {
        RestockChestData data = new RestockChestData();

        ListTag list = tag.getList("chests", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            RestockChestEntry entry = RestockChestEntry.load(list.getCompound(i));
            data.chests.put(entry.key(), entry);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (RestockChestEntry entry : this.chests.values()) {
            list.add(entry.save());
        }

        tag.put("chests", list);
        return tag;
    }

    public boolean contains(ResourceKey<Level> dimension, BlockPos pos) {
        return this.chests.containsKey(RestockChestEntry.key(dimension, pos));
    }

    public RestockChestEntry get(ResourceKey<Level> dimension, BlockPos pos) {
        return this.chests.get(RestockChestEntry.key(dimension, pos));
    }

    public Collection<RestockChestEntry> all() {
        return this.chests.values();
    }

    public void add(RestockChestEntry entry) {
        this.chests.put(entry.key(), entry);
        this.setDirty();
    }

    public void remove(ResourceKey<Level> dimension, BlockPos pos) {
        this.chests.remove(RestockChestEntry.key(dimension, pos));
        this.setDirty();
    }

    public void update(RestockChestEntry entry) {
        this.chests.put(entry.key(), entry);
        this.setDirty();
    }
}

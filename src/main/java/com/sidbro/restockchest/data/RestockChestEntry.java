package com.sidbro.restockchest.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RestockChestEntry(
        ResourceKey<Level> dimension,
        BlockPos pos,
        ResourceLocation lootTable,
        long cooldownTicks,
        long lastOpenedTicks,
        long nextRestockTicks,
        boolean isActive
) {
    public static RestockChestEntry create(
            ResourceKey<Level> dimension,
            BlockPos pos,
            ResourceLocation lootTable,
            long cooldownTicks
    ) {
        return new RestockChestEntry(
                dimension,
                pos,
                lootTable,
                cooldownTicks,
                -1L,
                -1L,
                false
        );
    }

    public String key() {
        return key(this.dimension, this.pos);
    }

    public static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + ":" + pos.asLong();
    }

    public RestockChestEntry startTimer(long gameTime) {
        return new RestockChestEntry(
                this.dimension,
                this.pos,
                this.lootTable,
                this.cooldownTicks,
                gameTime,
                gameTime + this.cooldownTicks,
                true
        );
    }

    public RestockChestEntry stopTimer() {
        return new RestockChestEntry(
                this.dimension,
                this.pos,
                this.lootTable,
                this.cooldownTicks,
                this.lastOpenedTicks,
                -1L,
                false
        );
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putString("dimension", this.dimension.location().toString());
        tag.putLong("pos", this.pos.asLong());
        tag.putString("lootTable", this.lootTable.toString());
        tag.putLong("cooldownTicks", this.cooldownTicks);
        tag.putLong("lastOpenedTicks", this.lastOpenedTicks);
        tag.putLong("nextRestockTicks", this.nextRestockTicks);
        tag.putBoolean("active", this.isActive);

        return tag;
    }

    public static RestockChestEntry load(CompoundTag tag) {
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension"))
        );

        return new RestockChestEntry(
                dimension,
                BlockPos.of(tag.getLong("pos")),
                ResourceLocation.parse(tag.getString("lootTable")),
                tag.getLong("cooldownTicks"),
                tag.getLong("lastOpenedTicks"),
                tag.getLong("nextRestockTicks"),
                tag.getBoolean("active")
        );
    }
}

package com.sidbro.restockchest.datagen;

import com.sidbro.restockchest.RestockChest;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class RestockChestLanguageProvider extends LanguageProvider {
    public RestockChestLanguageProvider(PackOutput output) {
        super(output, RestockChest.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(
                "command.restockchest.error.loot_table_not_found",
                "Loot table not found: %s"
        );

        add(
                "command.restockchest.error.container_not_found",
                "Container not found"
        );

        add(
                "command.restockchest.error.already_registered",
                "This container is already registered"
        );

        add(
                "command.restockchest.success.registered",
                "Container registered at %s with a cooldown of %s seconds"
        );

        add(
                "command.restockchest.error.not_registered",
                "This container is not registered"
        );

        add(
                "command.restockchest.success.removed",
                "Container registration removed at %s"
        );

        add(
                "command.restockchest.success.info_active",
                "Active container with %s loot table and %s cooldown ticks"
        );

        add(
                "command.restockchest.success.info_disabled",
                "Disabled container with %s loot table and %s cooldown ticks"
        );

        add(
                "command.restockchest.error.restock_failed",
                "Failed to restock this container"
        );

        add(
                "command.restockchest.success.restocked",
                "Container was restocked"
        );

        add(
                "command.restockchest.success.markers_enabled",
                "Highlighted chests within 8 chunks"
        );

        add(
                "command.restockchest.success.markers_disabled",
                "Removed chest markers"
        );

        add(
                "command.restockchest.error.no_loaded_chests",
                "No loaded chests were found within 8 chunks"
        );
        add(
                "chest.restockchest.time_left",
                "Restock in %s seconds"
        );
    }
}

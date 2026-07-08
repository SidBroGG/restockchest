package com.sidbro.restockchest.datagen;

import com.sidbro.restockchest.RestockChest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = RestockChest.MODID)
public class RestockChestDataGenerators {
    private RestockChestDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new RestockChestLanguageProvider(event.getGenerator().getPackOutput()));
    }
}

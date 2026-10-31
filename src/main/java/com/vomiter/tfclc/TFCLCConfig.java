package com.vomiter.tfclc;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@EventBusSubscriber(modid = TFCLostCities.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class TFCLCConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static ModConfigSpec.IntValue MIN_CITY_DISTANCE_FROM_SPAWN = BUILDER
            .comment("This value determines the minimum distance between world spawn and any city chunks.")
            .defineInRange("minCityDistanceFromSpawn", 0, 0, Integer.MAX_VALUE);
    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
    }
}

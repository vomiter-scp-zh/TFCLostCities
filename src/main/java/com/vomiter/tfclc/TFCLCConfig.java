package com.vomiter.tfclc;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = TFCLostCities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TFCLCConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec.IntValue MIN_CITY_DISTANCE_FROM_SPAWN = BUILDER
            .comment("This value determines the minimum distance between world spawn and any city chunks.")
            .defineInRange("minCityDistanceFromSpawn", 0, 0, Integer.MAX_VALUE);
    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
    }
}

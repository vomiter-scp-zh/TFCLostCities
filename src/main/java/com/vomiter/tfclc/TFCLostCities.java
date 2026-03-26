package com.vomiter.tfclc;

import com.mojang.logging.LogUtils;
import com.vomiter.tfclc.common.event.EventHandler;
import com.vomiter.tfclc.common.registry.ModRegistries;
import com.vomiter.tfclc.data.ModDataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TFCLostCities.MOD_ID)
public class TFCLostCities
{
    /**
     * TODO: CHECK AFC SAPLING ID for REMAPPER
     * TODO: GENERAL REPLACEMENT - TORCH, IRON DOOR, IRON BAR
     */

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "tfclc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation modLoc(String path){
        return Helpers.id(TFCLostCities.MOD_ID, path);
    }

    public TFCLostCities(FMLJavaModLoadingContext context) {
        EventHandler.init();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModDataGenerator::generateData);
        ModRegistries.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

}

package com.vomiter.tfclc.common.registry;

import com.vomiter.tfclc.TFCLostCities;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.Registry;

import java.util.ArrayList;
import java.util.List;

public class ModRegistries {
    static List<DeferredRegister<?>> REGISTRIES = new ArrayList<>();
    public static <B> DeferredRegister<B> createRegistry(Registry<B> b){
        return DeferredRegister.create(b, TFCLostCities.MOD_ID);
    }
    public static <B> DeferredRegister<B> createRegistry(ResourceKey<? extends Registry<B>> b){
        return DeferredRegister.create(b, TFCLostCities.MOD_ID);
    }

    static void add(DeferredRegister<?> r){
        REGISTRIES.add(r);
    }

    public static void register(IEventBus modBus){
        add(ModItems.ITEMS);
        add(ModLootFunctions.FUNCTIONS);
        add(TFCLCBlocks.BLOCKS);
        for (DeferredRegister<?> registry : REGISTRIES) {
            registry.register(modBus);
        }
    }
}

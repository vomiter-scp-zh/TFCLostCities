package com.vomiter.tfclc.common.registry;

import com.vomiter.tfclc.TFCLostCities;
import com.vomiter.tfclc.util.loot.BarrelContentFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType> FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, TFCLostCities.MOD_ID);
    public static final RegistryObject<LootItemFunctionType> BARREL_CONTENT = FUNCTIONS.register("barrel_content", () -> new LootItemFunctionType(new BarrelContentFunction.Serializer()));

    private ModLootFunctions() {}
}

package com.vomiter.tfclc.data.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.Collections;
import java.util.List;

public class ModLootTables extends LootTableProvider {
    public ModLootTables(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, Collections.emptySet(), List.of(
                new SubProviderEntry(SubLootTable::new, LootContextParamSets.CHEST)
        ), lookupProvider);
    }
}

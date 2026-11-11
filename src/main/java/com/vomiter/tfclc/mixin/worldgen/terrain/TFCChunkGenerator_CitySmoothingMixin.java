package com.vomiter.tfclc.mixin.worldgen.terrain;

import com.vomiter.tfclc.worldgen.CityTerrainSmoothingHelper;
import mcjty.lostcities.setup.Registration;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.LostCityFeature;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TFCChunkGenerator.class)
public abstract class TFCChunkGenerator_CitySmoothingMixin {

    // Shared edge samples read adjacent chunk biomes. Wait until FEATURES,
    // then smooth before city placement and TFC surface decoration.
    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    private void tfclc$smoothBeforeDecoration(
            WorldGenLevel world,
            ChunkAccess chunk,
            StructureManager structureManager,
            CallbackInfo ci
    ) {
        if (!(world instanceof WorldGenRegion level)) {
            return;
        }

        LostCityFeature feature = Registration.LOSTCITY_FEATURE.get();
        IDimensionInfo provider = feature.getDimensionInfo(level);

        if (provider == null) { return; }

        provider.setWorld(level);
        CityTerrainSmoothingHelper.smoothChunk(level, provider, (TFCChunkGenerator) (Object) this, chunk);
    }
}

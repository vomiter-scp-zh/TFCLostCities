package com.vomiter.tfclc.mixin;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.setup.Registration;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TFCChunkGenerator.class)
public abstract class TFCChunkGenerator_LostCitiesDebugMixin {

    @Inject(
        method = "applyBiomeDecoration",
        at = @At("TAIL")
    )
    private void tfclc$debugLostCitiesHook(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager,
            CallbackInfo ci
    ) {
        if (!(level instanceof WorldGenRegion region)) {
            return;
        }

        final ChunkPos center = region.getCenter();
        final ChunkPos current = chunk.getPos();
        if (current.x != center.x || current.z != center.z) {
            return;
        }

        if(true) return;

        final IDimensionInfo dimInfo = Registration.LOSTCITY_FEATURE.get().getDimensionInfo(level);
        LostCities.getLogger().info(
            "[TFCLostCities] Hook reached at chunk {}, {} | dim={} | dimInfo={}",
            current.x,
            current.z,
            level.getLevel().dimension().location(),
            dimInfo != null
        );
    }
}
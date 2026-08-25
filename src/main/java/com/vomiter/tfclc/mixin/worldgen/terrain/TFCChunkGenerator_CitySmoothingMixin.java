package com.vomiter.tfclc.mixin.worldgen.terrain;

import com.vomiter.tfclc.Helpers;
import com.vomiter.tfclc.worldgen.CityChunkData;
import com.vomiter.tfclc.worldgen.CityTerrainSmoothingHelper;
import mcjty.lostcities.setup.Registration;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.LostCityFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(TFCChunkGenerator.class)
public abstract class TFCChunkGenerator_CitySmoothingMixin {

    @Inject(
            method = "applyCarvers",
            at = @At("RETURN")
    )
    private void tfclc$smoothAfterCarvers(
            WorldGenRegion level,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving step,
            CallbackInfo ci
    ) {
        if (step != GenerationStep.Carving.AIR) {
            return;
        }


        LostCityFeature feature = Registration.LOSTCITY_FEATURE.get();
        IDimensionInfo provider = feature.getDimensionInfo(level);

        if (provider == null) { return; }

        provider.setWorld(level);
        ChunkPos chunkPos = chunk.getPos();
        ChunkCoord coord = new ChunkCoord(
                provider.getType(),
                chunkPos.x,
                chunkPos.z
        );

        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        boolean generatesCityTerrain = info.isCity || (info.outsideChunk && info.hasBuilding);
        CityChunkData chunkData = (CityChunkData) chunk;

        if (generatesCityTerrain) {
            chunkData.tfclc$setCityFloor(info.getCityGroundLevel());
            CityTerrainSmoothingHelper.lowerCurrentChunk(chunk, info.getCityGroundLevel());
        }

        Holder<Biome> centerBiome = chunk.getNoiseBiome(
                QuartPos.fromBlock(chunkPos.getMiddleBlockX()),
                QuartPos.fromBlock(info.getCityGroundLevel()),
                QuartPos.fromBlock(chunkPos.getMiddleBlockZ())
        );

        boolean biomeAllowed = centerBiome.is(TagKey.create(
                Registries.BIOME,
                Helpers.id("should_smooth")
        ));
        if (!biomeAllowed) return;

        OptionalInt targetHeight =
                CityTerrainSmoothingHelper.findTargetHeight(
                        provider,
                        (TFCChunkGenerator) (Object) this,
                        chunkPos
                );

        if (targetHeight.isEmpty()) {
            return;
        }

        if (CityTerrainSmoothingHelper.lowerCurrentChunk(chunk, targetHeight.getAsInt())) {
            chunkData.tfclc$setTerrainSmoothed(true);
            CityTerrainSmoothingHelper.primeAllHeightmaps(chunk);
        }
    }
}
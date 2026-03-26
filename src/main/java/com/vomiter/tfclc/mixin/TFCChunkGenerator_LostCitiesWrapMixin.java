package com.vomiter.tfclc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vomiter.tfclc.util.GenerationMarker;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.setup.Registration;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.feature.plant.FruitTreeFeature;
import net.dries007.tfc.world.feature.tree.ForestFeature;
import net.dries007.tfc.world.feature.tree.KrummholzFeature;
import net.dries007.tfc.world.feature.tree.OverlayTreeFeature;
import net.dries007.tfc.world.feature.tree.RandomTreeFeature;
import net.dries007.tfc.world.feature.tree.StackedTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TFCChunkGenerator.class)
public abstract class TFCChunkGenerator_LostCitiesWrapMixin {
    @Unique
    private static RandomSource tfclc$randomSource;

    @Unique
    private static final ThreadLocal<GenerationMarker> TFC_LOSTCITIES$MARKER = new ThreadLocal<>();

    @WrapOperation(
            method = "applyBiomeDecoration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;placeWithBiomeCheck(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean tfclc$wrapPlacedFeature(
            PlacedFeature placedFeature,
            WorldGenLevel featureLevel,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos originPos,
            Operation<Boolean> original,
            WorldGenLevel outerLevel,
            ChunkAccess chunk,
            StructureManager structureManager
    ) {
        if (!(outerLevel instanceof WorldGenRegion region)) {
            return original.call(placedFeature, featureLevel, generator, random, originPos);
        }

        final IDimensionInfo dimInfo = Registration.LOSTCITY_FEATURE.get().getDimensionInfo(outerLevel);
        if (dimInfo == null) {
            return original.call(placedFeature, featureLevel, generator, random, originPos);
        }

        tfclc$ensureLostCityGenerated(region, chunk, dimInfo);
        if(tfclc$randomSource == null) tfclc$randomSource = random.fork();

        var blockNatureChance = 0.5;
        if (tfclc$isBlockedNaturalTreeFeature(placedFeature)
                && tfclc$isInCityOrBuffer(chunk.getPos(), dimInfo, 1)
                && tfclc$randomSource.nextFloat() < blockNatureChance
        ) {
            return false;
        }

        return original.call(placedFeature, featureLevel, generator, random, originPos);
    }

    @Unique
    private static void tfclc$ensureLostCityGenerated(WorldGenRegion region, ChunkAccess chunk, IDimensionInfo dimInfo) {
        final ChunkPos center = region.getCenter();
        final ChunkPos current = chunk.getPos();

        if (!current.equals(center)) {
            return;
        }

        final GenerationMarker marker = TFC_LOSTCITIES$MARKER.get();
        if (marker != null && marker.matches(region, current)) {
            return;
        }

        try {
            if (region.getBiome(center.getMiddleBlockPosition(60)).is(Tags.Biomes.IS_VOID)) {
                return;
            }

            dimInfo.setWorld(region);
            dimInfo.getFeature().generate(region, region.getChunk(center.x, center.z));
            TFC_LOSTCITIES$MARKER.set(new GenerationMarker(region, current));
        } catch (Throwable t) {
            LostCities.getLogger().error(
                    "[TFCLostCities] Failed generating Lost Cities in chunk {}, {}",
                    current.x,
                    current.z,
                    t
            );
        }
    }

    @Unique
    private static boolean tfclc$isInCityOrBuffer(ChunkPos chunkPos, IDimensionInfo dimInfo, int bufferRadius) {
        for (int dx = -bufferRadius; dx <= bufferRadius; dx++) {
            for (int dz = -bufferRadius; dz <= bufferRadius; dz++) {
                final ChunkCoord coord = new ChunkCoord(dimInfo.getType(), chunkPos.x + dx, chunkPos.z + dz);
                if (BuildingInfo.isCity(coord, dimInfo)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private static boolean tfclc$isBlockedNaturalTreeFeature(PlacedFeature placedFeature) {
        final ConfiguredFeature<?, ?> configured = placedFeature.feature().value();
        final Feature<?> feature = configured.feature();

        // vanilla / 通用樹
        if (feature == Feature.TREE) {
            return true;
        }

        // TFC 樹系 feature：直接用 type 判斷
        return feature instanceof ForestFeature
                || feature instanceof FruitTreeFeature
                || feature instanceof RandomTreeFeature
                || feature instanceof OverlayTreeFeature
                || feature instanceof StackedTreeFeature
                || feature instanceof KrummholzFeature;
    }
}
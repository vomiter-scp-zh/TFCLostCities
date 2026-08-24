package com.vomiter.tfclc.mixin.worldgen.padding;

import com.vomiter.tfclc.common.PaddingResolver;
import com.vomiter.tfclc.common.registry.TFCLCBlocks;
import mcjty.lostcities.worldgen.ChunkDriver;
import mcjty.lostcities.worldgen.ChunkHeightmap;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.util.tuples.Pair;

import java.util.HashMap;

@Mixin(value = LostCityTerrainFeature.class, remap = false)
public abstract class LostCityTerrainFeature_PaddingMixin {
    @Shadow
    public abstract ChunkDriver getDriver();

    @Unique
    private static final int TFCLC_MIN_CAP_TOP = 100;

    @Unique
    private static final ThreadLocal<PaddingResolver.SurfaceColumn[]>
            tfclc$surfaceColumns = new ThreadLocal<>();

    @Unique
    private static int tfclc$columnIndex(int localX, int localZ) {
        return localX | localZ << 4;
    }

    @Inject(method = "doCityChunk", at = @At("HEAD"))
    private void tfclc$prepareCityFoundation(
            BuildingInfo info,
            ChunkHeightmap heightmap,
            ChunkAccess chunk,
            CallbackInfo ci
    ) {
        PaddingResolver.SurfaceColumn[] surfaceColumns =
                new PaddingResolver.SurfaceColumn[16 * 16];

        tfclc$surfaceColumns.set(surfaceColumns);

        int minBuildHeight = chunk.getMinBuildHeight();
        int maxBuildHeight = chunk.getMaxBuildHeight();

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = minX + localX;
                int z = minZ + localZ;

                PaddingResolver.SurfaceColumn original =
                        PaddingResolver.findOriginalSurface(chunk, x, z, pos);

                surfaceColumns[tfclc$columnIndex(localX, localZ)] = original;

                int capTop = Math.max(
                        TFCLC_MIN_CAP_TOP,
                        Math.max(
                                original.y() + 6,
                                info.getCityGroundLevel() + 6
                        )
                );

                capTop = Math.min(capTop, maxBuildHeight - 1);

                int paddingStart = Math.max(
                        heightmap.getHeight(),
                        minBuildHeight
                );

                for (int y = paddingStart; y <= capTop - 6; y++) {
                    pos.set(x, y, z);

                    if (chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(
                                pos,
                                TFCLCBlocks.CITY_PADDING.get().defaultBlockState(),
                                false
                        );
                    }
                }
            }
        }
    }

    @Inject(
            method = "doCityChunk",
            at = @At(
                    "TAIL"
            )
    )
    private void tfclc$resolveBeforeCityGeneration(
            BuildingInfo info,
            ChunkHeightmap heightmap,
            ChunkAccess chunk,
            CallbackInfo ci
    ) {
        PaddingResolver.SurfaceColumn[] surfaceColumns =
                tfclc$surfaceColumns.get();

        if (surfaceColumns == null) {
            throw new IllegalStateException(
                    "Missing padding surface context for chunk " + chunk.getPos()
            );
        }

        try {
            PaddingResolver.resolvePadding(info, chunk, getDriver(), surfaceColumns);
        } finally {
            tfclc$surfaceColumns.remove();
        }
    }}

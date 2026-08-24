package com.vomiter.tfclc.common;

import com.vomiter.tfclc.TFCLostCities;
import com.vomiter.tfclc.common.registry.TFCLCBlocks;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import mcjty.lostcities.worldgen.ChunkDriver;


public class PaddingResolver {
    public static void resolvePadding(
            BuildingInfo info,
            ChunkAccess chunk,
            ChunkDriver driver,
            SurfaceColumn[] surfaceColumns
    ) {
        Block paddingBlock = TFCLCBlocks.CITY_PADDING.get();

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState support = Blocks.GRAY_CONCRETE.defaultBlockState();

        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        int ground = info.getCityGroundLevel();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                SurfaceColumn column =
                        surfaceColumns[localX | (localZ << 4)];

                /*
                 * 清除城市高度以上的 padding。
                 */
                for (int y = maxY; y > ground; y--) {
                    BlockState state =
                            driver.getBlock(localX, y, localZ);

                    if (state.is(paddingBlock)) {
                        driver.current(localX, y, localZ);
                        driver.block(air);
                    }
                }

                int exposedPaddingY = Integer.MIN_VALUE;
                boolean covered = false;

                /*
                 * 此處必須讀取 driver，而不是 chunk。
                 * 否則判定依據和最後真正寫入世界的資料不是同一份。
                 */
                for (int y = maxY; y >= minY; y--) {
                    BlockState state =
                            driver.getBlock(localX, y, localZ);

                    if (state.isAir()) {
                        continue;
                    }

                    if (state.is(paddingBlock)) {
                        if (!covered) {
                            exposedPaddingY = y;
                        }
                        break;
                    }

                    covered = true;
                }

                for (int y = ground; y >= minY; y--) {
                    BlockState current =
                            driver.getBlock(localX, y, localZ);

                    if (!current.is(paddingBlock)) {
                        continue;
                    }

                    BlockState replacement;

                    if (y == exposedPaddingY) {
                        replacement = column.surface();
                    } else if (
                            y < exposedPaddingY
                                    && y > exposedPaddingY - 4
                    ) {
                        replacement = column.subsurface();
                    } else {
                        replacement = support;
                    }

                    TFCLostCities.LOGGER.info(
                            "Resolving ({}, {}, {}): {} -> {}",
                            localX,
                            y,
                            localZ,
                            current,
                            replacement
                    );

                    driver.current(localX, y, localZ);
                    driver.block(replacement);
                }
            }
        }
    }

    public static SurfaceColumn findOriginalSurface(
            ChunkAccess chunk,
            int x,
            int z,
            BlockPos.MutableBlockPos pos
    ) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;

        for (int y = maxY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState surface = chunk.getBlockState(pos);

            /*
             * 排除空氣、流體，以及 TFC ground cover、植物等非完整地表方塊。
             */
            if (surface.isAir()
                    || !surface.getFluidState().isEmpty()
                    || !surface.isSolidRender(chunk, pos)) {
                continue;
            }

            BlockState subsurface = surface;

            for (int belowY = y - 1; belowY >= minY; belowY--) {
                pos.set(x, belowY, z);
                BlockState candidate = chunk.getBlockState(pos);

                if (!candidate.isAir()
                        && candidate.getFluidState().isEmpty()
                        && candidate.isSolidRender(chunk, pos)) {
                    subsurface = candidate;
                    break;
                }
            }

            return new PaddingResolver.SurfaceColumn(y, surface, subsurface);
        }

        return new PaddingResolver.SurfaceColumn(
                minY,
                Blocks.AIR.defaultBlockState(),
                Blocks.AIR.defaultBlockState()
        );
    }


    public record SurfaceColumn(
            int y,
            BlockState surface,
            BlockState subsurface
    ) {
    }
}

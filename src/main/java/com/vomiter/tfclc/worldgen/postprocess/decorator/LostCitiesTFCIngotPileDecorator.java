package com.vomiter.tfclc.worldgen.postprocess.decorator;

import com.vomiter.tfclc.TFCLostCities;
import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import net.minecraft.resources.ResourceLocation;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.IngotPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LostCitiesTFCIngotPileDecorator {

    private LostCitiesTFCIngotPileDecorator() {
    }

    public static void decorateChunk(IDimensionInfo provider, ChunkCoord coord) {
        final LevelAccessor level = provider.getWorld();
        final BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        if (!provider.getProfile().GENERATE_LOOT || info.noLoot) {
            return;
        }

        final long seed = provider.getSeed() ^ Mth.getSeed(coord.chunkX(), info.getCityGroundLevel(), coord.chunkZ()) ^ 0x31D8F4B2A4C91E77L;
        final RandomSource random = RandomSource.create(seed);

        if (info.hasBuilding) {
            decorateBasement(level, info, random);
        }

        decorateRailSide(level, info, random);
    }

    private static void decorateBasement(LevelAccessor level, BuildingInfo info, RandomSource random) {
        final int cityGround = info.getCityGroundLevel();

        // 只掃地面下方一小段，避免亂插太深
        final int minY = Math.max(1, cityGround - 12);
        final int maxY = cityGround - 2;

        if (maxY < minY) {
            return;
        }

        final int targetPiles = 1 + random.nextInt(2); // 1~2
        int placed = 0;

        for (int i = 0; i < targetPiles * 16 && placed < targetPiles; i++) {
            final int rx = 2 + random.nextInt(12);
            final int rz = 2 + random.nextInt(12);
            final int y = minY + random.nextInt(maxY - minY + 1);

            final BlockPos pos = info.getRelativePos(rx, y, rz);

            if (!isGoodPileSpot(level, pos)) {
                continue;
            }
            if (!isBasementLike(level, pos)) {
                continue;
            }
            if (!isNearWall(level, pos, 1)) {
                continue;
            }
            if (isNearRail(level, pos, 2)) {
                continue;
            }

            if (tryPlaceIngotPile(level, pos, LostCitiesLoot.BASEMENT_INGOTS)) {
                TFCLostCities.LOGGER.debug("[TFCLC] Basement ingot pile placed at {}", pos);
                placed++;
            }
        }
    }

    private static void decorateRailSide(LevelAccessor level, BuildingInfo info, RandomSource random) {
        final int cityGround = info.getCityGroundLevel();

        // 地下鐵道、半露天鐵道都稍微涵蓋
        final int minY = Math.max(1, cityGround - 20);
        final int maxY = cityGround + 4;

        if (maxY < minY) {
            return;
        }

        final int targetPiles = 1 + random.nextInt(2); // 1~2
        int placed = 0;

        for (int i = 0; i < targetPiles * 20 && placed < targetPiles; i++) {
            final int rx = 1 + random.nextInt(14);
            final int rz = 1 + random.nextInt(14);
            final int y = minY + random.nextInt(maxY - minY + 1);

            final BlockPos pos = info.getRelativePos(rx, y, rz);

            if (!isGoodPileSpot(level, pos)) {
                continue;
            }
            if (!isNearRail(level, pos, 3)) {
                continue;
            }
            if (level.getBlockState(pos.below()).getBlock() instanceof BaseRailBlock) {
                continue;
            }

            if (tryPlaceIngotPile(level, pos, LostCitiesLoot.RAIL_INGOTS)) {
                TFCLostCities.LOGGER.debug("[TFCLC] Rail ingot pile placed at {}", pos);
                placed++;
            }
        }
    }

    private static boolean tryPlaceIngotPile(LevelAccessor level, BlockPos pos, ResourceLocation table) {
        final BlockState pileState = TFCBlocks.INGOT_PILE.get().defaultBlockState();
        if (!pileState.canSurvive(level, pos) || !level.setBlock(pos, pileState, 2)) {
            return false;
        }
        if (!(LostCitiesLoot.getOrCreateBlockEntity(level, pos) instanceof IngotPileBlockEntity)) {
            return false;
        }
        LostCitiesLoot.schedule(level, pos, table, false);
        return true;
    }

    private static boolean isGoodPileSpot(LevelAccessor level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        final BlockState above = level.getBlockState(pos.above());
        final BlockState below = level.getBlockState(pos.below());

        if (!state.canBeReplaced()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (!above.isAir()) {
            return false;
        }
        if (below.isAir()) {
            return false;
        }
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /**
     * 很粗略的 basement 粗判：
     * 1. 目前位置可站立
     * 2. 頭上至少有空間
     * 3. 周圍有一定比例不是空氣，避免插到露天坑洞
     */
    private static boolean isBasementLike(LevelAccessor level, BlockPos pos) {
        int solidCount = 0;
        int checked = 0;

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            cursor.set(pos.getX() + dir.getStepX(), pos.getY(), pos.getZ() + dir.getStepZ());
            checked++;
            if (!level.getBlockState(cursor).isAir()) {
                solidCount++;
            }

            cursor.set(pos.getX() + dir.getStepX(), pos.getY() + 1, pos.getZ() + dir.getStepZ());
            checked++;
            if (!level.getBlockState(cursor).isAir()) {
                solidCount++;
            }
        }

        return solidCount >= Math.max(3, checked / 2);
    }

    private static boolean isNearWall(LevelAccessor level, BlockPos pos, int radius) {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= radius; i++) {
                cursor.set(
                        pos.getX() + dir.getStepX() * i,
                        pos.getY(),
                        pos.getZ() + dir.getStepZ() * i
                );

                final BlockState state = level.getBlockState(cursor);
                if (!state.isAir() && state.isFaceSturdy(level, cursor, dir.getOpposite())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNearRail(LevelAccessor level, BlockPos center, int radius) {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    final BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof BaseRailBlock) {
                        return true;
                    }
                    if (state.is(Blocks.RAIL) || state.is(Blocks.POWERED_RAIL) || state.is(Blocks.DETECTOR_RAIL) || state.is(Blocks.ACTIVATOR_RAIL)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

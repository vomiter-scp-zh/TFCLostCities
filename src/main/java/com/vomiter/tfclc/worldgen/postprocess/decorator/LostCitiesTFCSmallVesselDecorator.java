package com.vomiter.tfclc.worldgen.postprocess.decorator;

import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.common.blockentities.PlacedItemBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.PlacedItemBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public final class LostCitiesTFCSmallVesselDecorator {
    private LostCitiesTFCSmallVesselDecorator() {}

    public static void decorateChunk(IDimensionInfo provider, ChunkCoord coord) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        if (!info.isCity || info.noLoot || !provider.getProfile().GENERATE_LOOT) {
            return;
        }
        LevelAccessor level = provider.getWorld();
        RandomSource random = RandomSource.create(provider.getSeed() ^ Mth.getSeed(coord.chunkX(), 0, coord.chunkZ()) ^ 0x4DC6B312AL);
        if (random.nextInt(3) != 0) {
            return;
        }
        int target = 1 + random.nextInt(3);
        int placed = 0;
        int minY = Math.max(level.getMinBuildHeight() + 1, info.getCityGroundLevel() - 12);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, Math.max(info.getCityGroundLevel() + 2, info.getMaxHeight()));
        for (int attempt = 0; attempt < 24 && placed < target; attempt++) {
            if (random.nextFloat() > 0.15f) continue;
            int x = 1 + random.nextInt(14);
            int z = 1 + random.nextInt(14);
            BlockPos selected = null;
            int candidates = 0;
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = info.getRelativePos(x, y, z);
                if (isGroundSpot(level, pos) && random.nextInt(++candidates) == 0) {
                    selected = pos;
                }
            }
            if (selected == null) {
                continue;
            }
            BlockState state = PlacedItemBlock.updateStateValues(level, selected.below(), TFCBlocks.PLACED_ITEM.get().defaultBlockState());
            if (level.setBlock(selected, state, 2) && LostCitiesLoot.getOrCreateBlockEntity(level, selected) instanceof PlacedItemBlockEntity) {
                LostCitiesLoot.schedule(level, selected, LostCitiesLoot.SMALL_VESSEL, false);
                placed++;
            }
        }
    }

    private static boolean isGroundSpot(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos.below()).getFluidState().isEmpty();
    }
}

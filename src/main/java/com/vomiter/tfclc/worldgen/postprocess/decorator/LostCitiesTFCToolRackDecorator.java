package com.vomiter.tfclc.worldgen.postprocess.decorator;

import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import com.vomiter.tfclc.TFCLostCities;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.common.blockentities.ToolRackBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.wood.ToolRackBlock;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LostCitiesTFCToolRackDecorator {

    private static final Wood[] RACK_WOODS = new Wood[] {
        Wood.OAK,
        Wood.SPRUCE,
        Wood.BIRCH,
        Wood.ASH,
        Wood.MAPLE,
        Wood.HICKORY
    };

    private LostCitiesTFCToolRackDecorator() {
    }

    public static void decorateChunk(IDimensionInfo provider, ChunkCoord coord) {
        final LevelAccessor level = provider.getWorld();
        final BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);

        if (!info.hasBuilding || info.noLoot || !provider.getProfile().GENERATE_LOOT) {
            return;
        }

        // 讓同一 chunk 結果穩定，不吃 runtime 隨機波動
        final long seed = provider.getSeed() ^ Mth.getSeed(coord.chunkX(), info.getCityGroundLevel(), coord.chunkZ()) ^ 0x54CFC7A1D93E4B17L;
        final RandomSource random = RandomSource.create(seed);

        final int minY = info.getCityGroundLevel() + 1;
        final int maxY = Math.max(minY, info.getMaxHeight() - 2);

        // 每個建築 chunk 嘗試放 3~5 個
        final int attempts = 3 + random.nextInt(3);
        int placed = 0;

        for (int i = 0; i < attempts * 12 && placed < attempts; i++) {
            final int rx = 2 + random.nextInt(12); // 避開 chunk 邊界
            final int rz = 2 + random.nextInt(12);
            final int y = random.nextInt(maxY - minY + 1) + minY;

            final BlockPos pos = info.getRelativePos(rx, y, rz);

            if (!isGoodCandidateAir(level, pos)) {
                continue;
            }
            if (!hasWalkableFloor(level, pos)) {
                continue;
            }

            final Direction facing = findWallFacing(level, pos, random);
            if (facing == null) {
                continue;
            }

            if (tryPlaceToolRack(level, pos, facing, random)) {
                placed++;
            }
        }
    }

    private static boolean tryPlaceToolRack(LevelAccessor level, BlockPos pos, Direction facing, RandomSource random) {
        final BlockState rackState = TFCBlocks.WOODS.get(randomWood(random))
            .get(Wood.BlockType.TOOL_RACK)
            .get()
            .defaultBlockState()
            .setValue(ToolRackBlock.FACING, facing);

        if (!rackState.canSurvive(level, pos)) {
            return false;
        }
        if (!level.setBlock(pos, rackState, 2)) {
            return false;
        }
        if (!(LostCitiesLoot.getOrCreateBlockEntity(level, pos) instanceof ToolRackBlockEntity rack)) {
            return false;
        }

        LostCitiesLoot.schedule(level, pos, LostCitiesLoot.TOOL_RACK, false);

        rack.setChanged();
        return true;
    }

    @Nullable
    private static Direction findWallFacing(LevelAccessor level, BlockPos pos, RandomSource random) {
        List<Direction> dirs = new ArrayList<>(4);
        dirs.add(Direction.NORTH);
        dirs.add(Direction.SOUTH);
        dirs.add(Direction.WEST);
        dirs.add(Direction.EAST);
        Collections.shuffle(dirs, new Random(random.nextLong()));

        for (Direction facing : dirs) {
            final BlockPos supportPos = pos.relative(facing.getOpposite());
            final BlockPos frontPos = pos.relative(facing);

            final BlockState supportState = level.getBlockState(supportPos);
            final BlockState frontState = level.getBlockState(frontPos);

            if (supportState.isAir()) {
                continue;
            }
            if (supportState.getBlock() instanceof DoorBlock) {
                continue;
            }
            if (!supportState.is(BlockTags.MINEABLE_WITH_PICKAXE)){ //避免掛在書架上
                continue;
            }
            if (!supportState.isFaceSturdy(level, supportPos, facing)) {
                continue;
            }
            if (supportState.is(Tags.Blocks.GLASS_BLOCKS)) {
                continue;
            }

            // 前方至少留一格空氣，避免掛在超窄夾層
            if (!frontState.isAir()) {
                continue;
            }

            return facing;
        }

        return null;
    }

    private static boolean isGoodCandidateAir(LevelAccessor level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        // 頭頂不要卡住，避免半格怪位置
        return level.getBlockState(pos.above()).isAir();
    }

    private static boolean hasWalkableFloor(LevelAccessor level, BlockPos pos) {
        final BlockPos below2 = pos.below().below();
        final BlockState floor = level.getBlockState(below2);
        final BlockState space = level.getBlockState(pos.below());
        return space.isAir() && !floor.isAir() && floor.isFaceSturdy(level, below2, Direction.UP);
    }

    private static Wood randomWood(RandomSource random) {
        return RACK_WOODS[random.nextInt(RACK_WOODS.length)];
    }

}

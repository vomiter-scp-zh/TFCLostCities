package com.vomiter.tfclc.util.bookshelf;

import net.dries007.tfc.common.blocks.wood.BookshelfBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class LostCitiesBookshelfFacingHelper {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private LostCitiesBookshelfFacingHelper() {}

    public static BlockState fixFacing(BlockGetter level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return state;
        return state.setValue(HorizontalDirectionalBlock.FACING, chooseBestFacing(level, pos));
    }

    public static Direction chooseBestFacing(BlockGetter level, BlockPos pos) {
        boolean alongX = shelf(level, pos, pos.west()) || shelf(level, pos, pos.east());
        boolean alongZ = shelf(level, pos, pos.north()) || shelf(level, pos, pos.south());
        List<BlockPos> row = new ArrayList<>();
        row.add(pos);
        Direction axis = alongX != alongZ ? (alongX ? Direction.EAST : Direction.SOUTH) : null;
        if (axis != null) {
            extend(level, pos, axis, row);
            extend(level, pos, axis.getOpposite(), row);
            extend(level, pos, Direction.UP, row, 3);
            extend(level, pos, Direction.DOWN, row, 3);
        }
        // The same anchor and initial-facing vote are used for every member of a straight row.
        BlockPos anchor = row.stream().min((a, b) -> Long.compare(a.asLong(), b.asLong())).orElse(pos);
        int offset = Math.floorMod(Mth.getSeed(anchor), 4);
        Direction best = level.getBlockState(anchor).getValue(HorizontalDirectionalBlock.FACING);
        int bestScore = Integer.MIN_VALUE;
        int bestVotes = -1;
        for (int i = 0; i < 4; i++) {
            Direction facing = HORIZONTAL[(i + offset) % 4];
            if (axis != null && facing.getAxis() == axis.getAxis()) continue;
            int score = 0;
            int votes = 0;
            for (BlockPos member : row) {
                score += scoreFacing(level, member, facing);
                if (level.getBlockState(member).getValue(HorizontalDirectionalBlock.FACING) == facing) votes++;
            }
            if (score > bestScore || (score == bestScore && votes > bestVotes)) {
                bestScore = score;
                bestVotes = votes;
                best = facing;
            }
        }
        return best;
    }

    private static void extend(BlockGetter level, BlockPos origin, Direction direction, List<BlockPos> row, int limit) {
        for (int distance = 1; distance < limit; distance++) {
            BlockPos candidate = origin.relative(direction, distance);
            if (!shelf(level, origin, candidate)) break;
            // Junctions belong to their own segment, rather than pulling two walls into one row.
            if (direction.getAxis() != Direction.Axis.Y
                    && (shelf(level, origin, candidate.relative(direction.getClockWise()))
                    || shelf(level, origin, candidate.relative(direction.getCounterClockWise())))) break;
            row.add(candidate);
        }
    }


    private static void extend(BlockGetter level, BlockPos origin, Direction direction, List<BlockPos> row) {
        extend(level, origin, direction, row, 16);
    }

    private static int scoreFacing(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        BlockPos back = pos.relative(facing.getOpposite());
        // Never request an adjacent generation chunk or interpret unavailable space as air.
        if (!inChunk(pos, front)) return -200;
        int score = clear(level, front) ? 40 : -1000;
        if (standingSpace(level, front) || standingSpace(level, front.below())) score += 300;
        if (inChunk(pos, back)) {
            BlockState backing = level.getBlockState(back);
            if (!(backing.getBlock() instanceof BookshelfBlock) && backing.isFaceSturdy(level, back, facing)) score += 100;
        }
        // Count usable floor space, not a long view through a window or a ruined exterior wall.
        for (int distance = 2; distance <= 4; distance++) {
            BlockPos next = pos.relative(facing, distance);
            if (!inChunk(pos, next) || !clear(level, next)) break;
            if (standingSpace(level, next) || standingSpace(level, next.below())) score += 15;
        }
        return score;
    }

    private static boolean standingSpace(BlockGetter level, BlockPos feet) {
        BlockPos floor = feet.below();
        return clear(level, feet) && clear(level, feet.above())
                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    private static boolean clear(BlockGetter level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static boolean shelf(BlockGetter level, BlockPos origin, BlockPos pos) {
        return inChunk(origin, pos) && level.getBlockState(pos).getBlock() instanceof BookshelfBlock;
    }

    private static boolean inChunk(BlockPos origin, BlockPos pos) {
        return origin.getX() >> 4 == pos.getX() >> 4 && origin.getZ() >> 4 == pos.getZ() >> 4;
    }
}

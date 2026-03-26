package com.vomiter.tfclc.worldgen.remapper;

import com.vomiter.tfclc.worldgen.remapper.wood.LostCitiesWoodRemapper;
import com.vomiter.tfclc.worldgen.postprocess.LostCitiesPostProcessTracker;
import com.vomiter.tfclc.worldgen.postprocess.PendingBlockEntityKind;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.Plant;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LostCitiesGeneralRemapper {
    public static BlockState remap(BlockState state, BlockPos pos, LevelAccessor region) {
        if (state == null) {
            return null;
        }

        if(state.is(Blocks.BOOKSHELF)){
            LostCitiesPostProcessTracker.add(pos, PendingBlockEntityKind.FILLED_BOOKSHELF);
            return TFCBlocks.WOODS.get(Wood.OAK).get(Wood.BlockType.BOOKSHELF).get().defaultBlockState();
        }

        if(state.is(Blocks.GLOWSTONE)){
            LostCitiesPostProcessTracker.add(pos, PendingBlockEntityKind.LAVA_LAMP);
            return TFCBlocks.METALS.get(Metal.Default.BLUE_STEEL).get(Metal.BlockType.LAMP).get().defaultBlockState();
        }

        if(state.is(Blocks.ENCHANTING_TABLE)){
            return TFCBlocks.METALS
                    .get(Metal.Default.BISMUTH_BRONZE)
                    .get(Metal.BlockType.ANVIL)
                    .get()
                    .defaultBlockState();
        }

        if(state.is(BlockTags.ANVIL)){
            return TFCBlocks.METALS
                    .get(Metal.Default.BLACK_BRONZE)
                    .get(Metal.BlockType.ANVIL)
                    .get()
                    .defaultBlockState();
        }

        if(state.is(Blocks.CHEST) || state.is(Blocks.BARREL)){
            return TFCBlocks.LARGE_VESSEL.get().defaultBlockState();
        }

        if(state.is(BlockTags.FLOWER_POTS)){
            if(state.is(Blocks.POTTED_CACTUS)) return TFCBlocks.POTTED_PLANTS.get(Plant.BARREL_CACTUS).get().defaultBlockState();
            else return TFCBlocks.POTTED_PLANTS.get(Plant.DEAD_BUSH).get().defaultBlockState();
        }

        if(state.is(Blocks.FURNACE)){
            return TFCBlocks.CRUCIBLE.get().defaultBlockState();
        }

        if(state.is(Blocks.WALL_TORCH)){
            return TFCBlocks.DEAD_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING ,state.getValue(WallTorchBlock.FACING));
        }

        if(state.is(Blocks.TORCH)){
            return TFCBlocks.DEAD_TORCH.get().defaultBlockState();
        }

        if(state.is(Blocks.IRON_BARS)){
            return TFCBlocks.METALS
                    .get(Metal.Default.WROUGHT_IRON)
                    .get(Metal.BlockType.BARS).get()
                    .defaultBlockState()
                    .setValue(IronBarsBlock.EAST, state.getValue(IronBarsBlock.EAST))
                    .setValue(IronBarsBlock.WEST, state.getValue(IronBarsBlock.WEST))
                    .setValue(IronBarsBlock.NORTH, state.getValue(IronBarsBlock.NORTH))
                    .setValue(IronBarsBlock.SOUTH, state.getValue(IronBarsBlock.SOUTH));
        }

        if(state.is(Blocks.CHAIN)){
            return TFCBlocks.METALS
                    .get(Metal.Default.WROUGHT_IRON)
                    .get(Metal.BlockType.CHAIN).get()
                    .defaultBlockState()
                    .setValue(ChainBlock.AXIS, state.getValue(ChainBlock.AXIS));
        }

        var woodRemap = LostCitiesWoodRemapper.remapVanillaWood(state);
        if(woodRemap != null && woodRemap != state) return woodRemap;

        return state;
    }
}

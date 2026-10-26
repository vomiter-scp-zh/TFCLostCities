package com.vomiter.tfclc.worldgen.remapper;

import com.vomiter.tfclc.Helpers;
import com.vomiter.tfclc.worldgen.remapper.wood.LostCitiesWoodRemapper;
import com.vomiter.tfclc.worldgen.postprocess.LostCitiesPostProcessTracker;
import com.vomiter.tfclc.worldgen.postprocess.PendingBlockEntityKind;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.Plant;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.dries007.tfc.common.blocks.devices.BarrelBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

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
            LostCitiesPostProcessTracker.add(pos, PendingBlockEntityKind.LOOT_CONTAINER);
            long seed = region instanceof WorldGenLevel world ? world.getSeed() : 0L;
            if (pos != null && RandomSource.create(seed ^ pos.asLong() ^ 0x58F1D3A91CL).nextBoolean()) {
                BlockState chest = TFCBlocks.WOODS.get(Wood.OAK).get(Wood.BlockType.CHEST).get().defaultBlockState();
                return state.hasProperty(ChestBlock.FACING) ? chest.setValue(ChestBlock.FACING, state.getValue(ChestBlock.FACING)) : chest;
            }
            return TFCBlocks.LARGE_VESSEL.get().defaultBlockState();
        }

        if(state.is(BlockTags.FLOWER_POTS)){
            if(state.is(Blocks.POTTED_CACTUS)) return TFCBlocks.POTTED_PLANTS.get(Plant.BARREL_CACTUS).get().defaultBlockState();
            else return TFCBlocks.POTTED_PLANTS.get(Plant.DEAD_BUSH).get().defaultBlockState();
        }

        if(state.is(Blocks.FURNACE)){
            long seed = region instanceof WorldGenLevel world ? world.getSeed() : 0L;
            if (pos != null && RandomSource.create(seed ^ pos.asLong() ^ 0x61C24A9BL).nextInt(3) == 0) {
                LostCitiesPostProcessTracker.add(pos, PendingBlockEntityKind.LOOT_CONTAINER);
                return TFCBlocks.WOODS.get(Wood.OAK).get(Wood.BlockType.BARREL).get().defaultBlockState()
                        .setValue(BarrelBlock.FACING, Direction.UP);
            }
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

        if(state.is(Blocks.OAK_PLANKS)){
            return TFCBlocks.WOODS.get(Wood.OAK).get(Wood.BlockType.PLANKS).get().defaultBlockState();
        }

        if(state.is(Blocks.DEEPSLATE_TILES) || state.is(Blocks.CRACKED_DEEPSLATE_TILES)){
            return TFCBlocks.ROCK_BLOCKS.get(Rock.SHALE).get(Rock.BlockType.CHISELED).get().defaultBlockState();
        }

        if(state.is(Blocks.IRON_BLOCK)){
            return TFCBlocks.METALS.get(Metal.Default.WROUGHT_IRON).get(Metal.BlockType.BLOCK).get().defaultBlockState();
        }


        var woodRemap = LostCitiesWoodRemapper.remapVanillaWood(state);
        if(woodRemap != null && woodRemap != state) return woodRemap;
        var TFCIsOcean = TagKey.create(
                ForgeRegistries.BIOMES.getRegistryKey(),
                Helpers.id("tfc", "is_ocean")
        );

        if (state.is(Blocks.WATER) && pos != null && region.getBiome(pos).is(TFCIsOcean)){
            return TFCBlocks.SALT_WATER.get().defaultBlockState();
        }

        return state;
    }
}

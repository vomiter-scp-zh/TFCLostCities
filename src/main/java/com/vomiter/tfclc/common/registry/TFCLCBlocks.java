package com.vomiter.tfclc.common.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TFCLCBlocks {
    public static final DeferredRegister<Block> BLOCKS
            = ModRegistries.createRegistry(ForgeRegistries.BLOCKS);

    public static final RegistryObject<Block> CITY_PADDING = BLOCKS.register(
            "city_padding",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GLASS))
    );
}

package com.vomiter.tfclc.mixin.worldgen.remap;

import com.vomiter.tfclc.worldgen.remapper.LostCitiesStoneRemapper;
import mcjty.lostcities.worldgen.ChunkDriver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ChunkDriver.class, remap = false)
public abstract class ChunkDriver_TFCStoneRemapMixin {

    @ModifyVariable(
            method = "block(Lnet/minecraft/world/level/block/state/BlockState;)Lmcjty/lostcities/worldgen/ChunkDriver;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState tfclc$remapBlock(BlockState state) {
        return LostCitiesStoneRemapper.remap(state);
    }

    @ModifyVariable(
            method = "add(Lnet/minecraft/world/level/block/state/BlockState;)Lmcjty/lostcities/worldgen/ChunkDriver;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState tfclc$remapAdd(BlockState state) {
        return LostCitiesStoneRemapper.remap(state);
    }

    @ModifyVariable(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState tfclc$remapSetBlock(BlockState state) {
        return LostCitiesStoneRemapper.remap(state);
    }

    @ModifyVariable(
            method = "setBlockRange(IIIILnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState tfclc$remapSetBlockRange(BlockState state) {
        return LostCitiesStoneRemapper.remap(state);
    }

    @ModifyVariable(
            method = "setBlockRange(IIIILnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Predicate;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockState tfclc$remapSetBlockRangeWithPredicate(BlockState state) {
        return LostCitiesStoneRemapper.remap(state);
    }
}
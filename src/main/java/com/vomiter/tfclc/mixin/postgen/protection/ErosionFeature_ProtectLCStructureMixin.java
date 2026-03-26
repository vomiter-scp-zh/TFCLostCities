package com.vomiter.tfclc.mixin.postgen.protection;

import com.vomiter.tfclc.util.stone.LostCitiesProtectionTracker;
import net.dries007.tfc.world.feature.ErosionFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ErosionFeature.class, remap = false)
public abstract class ErosionFeature_ProtectLCStructureMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "HEAD", remap = true),
            cancellable = true
    )
    private void tfclc$skipProtectedLostCitiesBlocks(
            WorldGenLevel level,
            ChunkAccess chunk,
            BlockPos pos,
            BlockState newState,
            CallbackInfo ci
    ) {
        if (LostCitiesProtectionTracker.isProtected(chunk, pos)) {
            ci.cancel();
        }
    }
}
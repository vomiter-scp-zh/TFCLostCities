package com.vomiter.tfclc.mixin.postgen.protection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vomiter.tfclc.util.stone.LostCitiesProtectionTracker;
import net.dries007.tfc.world.feature.vein.VeinFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VeinFeature.class, remap = false)
public abstract class VeinFeature_ProtectLCStructureMixin {

    @WrapOperation(
            method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;IILnet/dries007/tfc/world/feature/vein/IVein;Lnet/dries007/tfc/world/feature/vein/IVeinConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    remap = true
            )
    )
    private boolean tfclc$skipProtectedLostCitiesBlocks(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original
    ) {
        if (LostCitiesProtectionTracker.isProtected(pos)) {
            return false;
        }
        return original.call(level, pos, state, flags);
    }
}
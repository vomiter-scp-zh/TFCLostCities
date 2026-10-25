package com.vomiter.tfclc.mixin.postgen.decoration;

import com.vomiter.tfclc.worldgen.postprocess.decorator.LostCitiesTFCSmallVesselDecorator;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.ChunkFixer;
import mcjty.lostcities.worldgen.IDimensionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkFixer.class, remap = false)
public abstract class ChunkFixer_TFCSmallVesselDecoratorMixin {
    @Inject(method = "fix", at = @At("TAIL"))
    private static void tfclc$decorateSmallVessels(IDimensionInfo info, ChunkCoord coord, CallbackInfo ci) {
        LostCitiesTFCSmallVesselDecorator.decorateChunk(info, coord);
    }
}

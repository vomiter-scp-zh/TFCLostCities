package com.vomiter.tfclc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mcjty.lostcities.worldgen.lost.regassets.data.BiomeMatcher;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BiomeMatcher.class, remap = false)
public class BiomeMatcherMixin {
    @WrapMethod(method = "test(Lnet/minecraft/core/Holder;)Z")
    private boolean tfclc$testTFCBiome(Holder<Biome> biome, Operation<Boolean> original){
        return original.call(biome);
    }
}

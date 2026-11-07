package com.vomiter.tfclc.mixin.postgen;

import com.vomiter.tfclc.util.LostCitiesLamps;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// onLoad is declared on TFCBlockEntity and inherited by LampBlockEntity.
@Mixin(value = TFCBlockEntity.class, remap = false)
public abstract class TFCBlockEntity_PendingLampMixin {
    @Inject(method = "onLoad", at = @At("TAIL"))
    private void tfclc$initializePendingLamp(CallbackInfo ci) {
        LostCitiesLamps.initialize((TFCBlockEntity) (Object) this);
    }
}

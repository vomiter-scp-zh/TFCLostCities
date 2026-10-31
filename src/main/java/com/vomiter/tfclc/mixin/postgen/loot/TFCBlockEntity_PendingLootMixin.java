package com.vomiter.tfclc.mixin.postgen.loot;

import com.vomiter.tfclc.util.loot.IPendingLootVessel;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TFCBlockEntity.class, remap = false)
public abstract class TFCBlockEntity_PendingLootMixin implements IPendingLootVessel {

    @Unique
    private static final String TFCLC_PENDING_LOOT_ID = "tfclc_pending_loot_id";
    @Unique
    private static final String TFCLC_PENDING_SEAL = "tfclc_pending_seal";
    @Unique
    private static final String TFCLC_PENDING_INIT = "tfclc_pending_init";
    @Unique
    private static final String TFCLC_LOOT_INITIALIZED = "tfclc_loot_initialized";

    @Unique
    @Nullable
    private ResourceLocation tfclc$pendingLootId;

    @Unique
    private boolean tfclc$pendingSeal;

    @Unique
    private boolean tfclc$pendingInit;

    @Unique
    private boolean tfclc$lootInitialized;

    @Override
    public void tfclc$setPendingLoot(ResourceLocation lootId, boolean sealAfterFill) {
        if (tfclc$lootInitialized || tfclc$hasPendingLoot()) {
            return;
        }
        this.tfclc$pendingLootId = lootId;
        this.tfclc$pendingSeal = sealAfterFill;
        this.tfclc$pendingInit = true;
        ((TFCBlockEntity) (Object) this).setChanged();
    }

    @Override
    public boolean tfclc$hasPendingLoot() {
        return tfclc$pendingInit && tfclc$pendingLootId != null && !tfclc$lootInitialized;
    }

    @Override
    public ResourceLocation tfclc$getPendingLootId() {
        return tfclc$pendingLootId;
    }

    @Override
    public boolean tfclc$shouldSealAfterFill() {
        return tfclc$pendingSeal;
    }

    @Override
    public boolean tfclc$isLootInitialized() {
        return tfclc$lootInitialized;
    }

    @Override
    public void tfclc$setLootInitialized(boolean initialized) {
        this.tfclc$lootInitialized = initialized;
    }

    @Override
    public void tfclc$clearPendingLoot() {
        this.tfclc$pendingLootId = null;
        this.tfclc$pendingSeal = false;
        this.tfclc$pendingInit = false;
    }

    @Override
    public void tfclc$tryInitializePendingLoot() {
        if (!tfclc$hasPendingLoot()) {
            return;
        }

        TFCBlockEntity self = (TFCBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.getServer().isSameThread()) {
            serverLevel.getServer().execute(() -> {
                if (!self.isRemoved() && serverLevel.getBlockEntity(self.getBlockPos()) == self) {
                    tfclc$tryInitializePendingLoot();
                }
            });
            return;
        }

        if (!LostCitiesLoot.initialize(serverLevel, self, tfclc$pendingLootId, tfclc$pendingSeal)) {
            return;
        }

        this.tfclc$lootInitialized = true;
        this.tfclc$clearPendingLoot();
        self.setChanged();
    }

    // ProtoChunk block entities intentionally have no Level until chunk loading.
    // Keep markForSync's setChanged(), but do not build a packet before attachment.
    // Inventory and fluid callbacks can reach this path during world generation too.
    @Inject(method = "sendVanillaUpdatePacket", at = @At("HEAD"), cancellable = true)
    private void tfclc$skipUnattachedSync(CallbackInfo ci) {
        if (((TFCBlockEntity) (Object) this).getLevel() == null) {
            ci.cancel();
        }
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void tfclc$onLoadTryInit(CallbackInfo ci) {
        tfclc$tryInitializePendingLoot();
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void tfclc$loadPendingLoot(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (tag.contains(TFCLC_PENDING_LOOT_ID)) {
            this.tfclc$pendingLootId = ResourceLocation.tryParse(tag.getString(TFCLC_PENDING_LOOT_ID));
        } else {
            this.tfclc$pendingLootId = null;
        }
        this.tfclc$pendingSeal = tag.getBoolean(TFCLC_PENDING_SEAL);
        this.tfclc$pendingInit = tag.getBoolean(TFCLC_PENDING_INIT);
        this.tfclc$lootInitialized = tag.getBoolean(TFCLC_LOOT_INITIALIZED);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void tfclc$savePendingLoot(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this.tfclc$pendingLootId != null) {
            tag.putString(TFCLC_PENDING_LOOT_ID, this.tfclc$pendingLootId.toString());
        }
        tag.putBoolean(TFCLC_PENDING_SEAL, this.tfclc$pendingSeal);
        tag.putBoolean(TFCLC_PENDING_INIT, this.tfclc$pendingInit);
        tag.putBoolean(TFCLC_LOOT_INITIALIZED, this.tfclc$lootInitialized);
    }
}

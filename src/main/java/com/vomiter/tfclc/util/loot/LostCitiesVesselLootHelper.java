package com.vomiter.tfclc.util.loot;

import com.vomiter.tfclc.TFCLostCities;
import net.dries007.tfc.common.blockentities.LargeVesselBlockEntity;
import net.dries007.tfc.common.blocks.LargeVesselBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

public final class LostCitiesVesselLootHelper {

    private LostCitiesVesselLootHelper() {
    }

    public static void initializePendingLoot(
            ServerLevel level,
            LargeVesselBlockEntity vessel,
            ResourceLocation lootId,
            boolean sealAfterFill
    ) {
        BlockPos pos = vessel.getBlockPos();

        List<ItemStack> items = LostCitiesLoot.roll(level, pos, lootId);

        var handler = vessel.getInventory();
        clear(handler);

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remain = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);

            if (!remain.isEmpty()) {
                TFCLostCities.LOGGER.warn("Vessel loot {} could not fit {} at {}", lootId, remain, pos);
            }
        }

        vessel.setChanged();

        if (sealAfterFill) {
            sealLargeVessel(level, pos);
        }
    }

    public static void sealLargeVessel(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (!state.hasProperty(LargeVesselBlock.SEALED)) {
            return;
        }

        if (!state.getValue(LargeVesselBlock.SEALED)) {
            level.setBlock(pos, state.setValue(LargeVesselBlock.SEALED, true), 3);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LargeVesselBlockEntity sealedVessel) {
                sealedVessel.onSeal();
                sealedVessel.setChanged();
            }
        }
    }

    private static void clear(IItemHandler handler) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            for (int i = 0; i < modifiable.getSlots(); i++) {
                modifiable.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}

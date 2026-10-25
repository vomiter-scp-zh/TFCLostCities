package com.vomiter.tfclc.util.loot;

import com.vomiter.tfclc.Helpers;
import com.vomiter.tfclc.TFCLostCities;
import net.dries007.tfc.common.blockentities.LargeVesselBlockEntity;
import net.dries007.tfc.common.blockentities.BarrelBlockEntity;
import net.dries007.tfc.common.blocks.devices.BarrelBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.blockentities.ToolRackBlockEntity;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

public final class LostCitiesLoot {
    public static final ResourceLocation TOOL_RACK = Helpers.id("tool_rack/default");
    public static final ResourceLocation BARREL = Helpers.id("barrel/default");
    public static final ResourceLocation CHEST = Helpers.id("chest/default");
    public static final ResourceLocation LARGE_VESSEL = Helpers.id("large_vessel/default");

    private LostCitiesLoot() {}

    public static void schedule(LevelAccessor level, BlockPos pos, ResourceLocation table, boolean seal) {
        if (level.getBlockEntity(pos) instanceof IPendingLootVessel pending) {
            pending.tfclc$setPendingLoot(table, seal);
            // Proto-chunk entities initialize onLoad; already loaded entities initialize now.
            if (level instanceof ServerLevel) {
                pending.tfclc$tryInitializePendingLoot();
            }
        }
    }

    public static boolean scheduleContainer(LevelAccessor level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TFCChestBlockEntity chest) {
            long seed = level instanceof WorldGenLevel world ? world.getSeed() : 0L;
            chest.setLootTable(CHEST, seed ^ pos.asLong() ^ ((long) CHEST.hashCode() << 32));
            chest.setChanged();
            return true;
        }
        if (level.getBlockEntity(pos) instanceof LargeVesselBlockEntity) {
            schedule(level, pos, LARGE_VESSEL, true);
            return true;
        }
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity) {
            schedule(level, pos, BARREL, true);
            return true;
        }
        return false;
    }

    public static long seed(ServerLevel level, BlockPos pos, ResourceLocation table) {
        return level.getSeed() ^ pos.asLong() ^ ((long) table.hashCode() << 32);
    }

    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, ResourceLocation table) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        return level.getServer().getLootData().getLootTable(table).getRandomItems(params, seed(level, pos, table));
    }

    public static boolean initialize(ServerLevel level, TFCBlockEntity entity, ResourceLocation table, boolean seal) {
        if (entity instanceof LargeVesselBlockEntity vessel) {
            LostCitiesVesselLootHelper.initializePendingLoot(level, vessel, table, seal);
            return true;
        }
        if (entity instanceof BarrelBlockEntity barrel) {
            fillBarrel(level, barrel, table, seal);
            return true;
        }
        if (entity instanceof ToolRackBlockEntity rack) {
            rack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (ItemStack stack : roll(level, rack.getBlockPos(), table)) {
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                    if (!remainder.isEmpty()) {
                        TFCLostCities.LOGGER.warn("Tool rack loot {} could not fit {} at {}", table, remainder, rack.getBlockPos());
                    }
                }
            });
            rack.markForBlockUpdate();
            return true;
        }
        return false;
    }
    private static void fillBarrel(ServerLevel level, BarrelBlockEntity barrel, ResourceLocation table, boolean seal) {
        // Only the main item slot is storage. Slots 0/1 are fluid-container IO.
        for (ItemStack generated : roll(level, barrel.getBlockPos(), table)) {
            ItemStack stack = generated.copy();
            CompoundTag payload = stack.getTag() == null ? new CompoundTag() : stack.getTag().getCompound(BarrelContentFunction.PAYLOAD);
            boolean fluidOnly = payload.getBoolean("fluid_only");
            stack.removeTagKey(BarrelContentFunction.PAYLOAD);
            if (payload.contains("fluid")) {
                ResourceLocation fluidId = ResourceLocation.tryParse(payload.getString("fluid"));
                if (fluidId != null && ForgeRegistries.FLUIDS.containsKey(fluidId)) {
                    FluidStack fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(fluidId), payload.getInt("amount"));
                    barrel.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
                        int filled = handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                        if (filled != fluid.getAmount()) {
                            TFCLostCities.LOGGER.warn("Barrel loot {} accepted only {}/{} mB at {}", table, filled, fluid.getAmount(), barrel.getBlockPos());
                        }
                    });
                }
            }
            if (!fluidOnly && !stack.isEmpty()) {
                barrel.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    ItemStack remainder = handler.insertItem(BarrelBlockEntity.SLOT_ITEM, stack, false);
                    if (!remainder.isEmpty()) {
                        TFCLostCities.LOGGER.warn("Barrel loot {} could not fit {} at {}", table, remainder, barrel.getBlockPos());
                    }
                });
            }
        }
        if (seal && !barrel.getBlockState().getValue(BarrelBlock.SEALED)) {
            level.setBlock(barrel.getBlockPos(), barrel.getBlockState().setValue(BarrelBlock.SEALED, true), 3);
            barrel.onSeal();
        }
        barrel.markForBlockUpdate();
    }

}

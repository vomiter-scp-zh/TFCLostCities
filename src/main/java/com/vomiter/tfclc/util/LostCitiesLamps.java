package com.vomiter.tfclc.util;

import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import net.dries007.tfc.common.blockentities.LampBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.LampBlock;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** One-time initialization of generated blue steel lamps, after level attachment. */
public final class LostCitiesLamps {
    private static final String PENDING = "tfclc_lava_lamp_pending";
    private static final String INITIALIZED = "tfclc_lava_lamp_initialized";

    private LostCitiesLamps() {}

    public static void schedule(LevelAccessor level, BlockPos pos) {
        if (!isBlueSteelLamp(level.getBlockState(pos))) return;
        // WorldGenRegion#getBlockEntity warns when LC has only written section states.
        // Register a real entity through the chunk, without assigning a fake Level.
        BlockEntity entity = LostCitiesLoot.getOrCreateBlockEntity(level, pos);
        if (!(entity instanceof LampBlockEntity lamp)) return;
        if (lamp.getLevel() instanceof ServerLevel server && !server.getServer().isSameThread()) {
            BlockPos savedPos = pos.immutable();
            server.getServer().execute(() -> {
                if (!lamp.isRemoved() && server.hasChunkAt(savedPos) && server.getBlockEntity(savedPos) == lamp) {
                    schedule(server, savedPos);
                }
            });
            return;
        }
        CompoundTag data = lamp.getPersistentData();
        if (data.getBoolean(INITIALIZED)) return;
        data.putBoolean(PENDING, true);
        lamp.setChanged();
        if (level instanceof WorldGenRegion) level.getChunk(pos).setUnsaved(true);
        initialize(lamp);
    }

    /** Called from TFCBlockEntity.onLoad, including entities restored from chunk NBT. */
    public static void initialize(BlockEntity entity) {
        if (!(entity instanceof LampBlockEntity lamp)
                || !(lamp.getLevel() instanceof ServerLevel server)) return;
        if (!server.getServer().isSameThread()) {
            server.getServer().execute(() -> initialize(lamp));
            return;
        }
        CompoundTag data = lamp.getPersistentData();
        if (!data.getBoolean(PENDING) || data.getBoolean(INITIALIZED)) return;
        BlockPos pos = lamp.getBlockPos();
        if (lamp.isRemoved() || !server.hasChunkAt(pos) || server.getBlockEntity(pos) != lamp) return;
        BlockState state = server.getBlockState(pos);
        if (!isBlueSteelLamp(state)) {
            data.remove(PENDING);
            lamp.setChanged();
            return;
        }
        IFluidHandler tank = lamp.getSidedFluidInventory(null);
        if (tank == null) return;
        // Preserve any saved fuel instead of topping it up on every load or retry.
        if (tank.getFluidInTank(0).isEmpty()) {
            tank.fill(new FluidStack(Fluids.LAVA, 250), IFluidHandler.FluidAction.EXECUTE);
        }
        FluidStack fuel = tank.getFluidInTank(0);
        if (fuel.isEmpty()) return;
        if (fuel.is(Fluids.LAVA)) {
            lamp.resetCounter();
            if (!state.getValue(LampBlock.LIT)
                    && !server.setBlock(pos, state.setValue(LampBlock.LIT, true), 2)) return;
        }
        data.putBoolean(INITIALIZED, true);
        data.remove(PENDING);
        lamp.markForSync();
    }

    private static boolean isBlueSteelLamp(BlockState state) {
        return state.is(TFCBlocks.METALS.get(Metal.BLUE_STEEL).get(Metal.BlockType.LAMP).get());
    }
}

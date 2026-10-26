package com.vomiter.tfclc.mixin.postgen;

import com.vomiter.tfclc.TFCLostCities;
import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import com.vomiter.tfclc.util.bookshelf.LostCitiesBookshelves;
import com.vomiter.tfclc.worldgen.postprocess.LostCitiesPostProcessTracker;
import com.vomiter.tfclc.worldgen.postprocess.PendingBlockEntityKind;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.ChunkFixer;
import mcjty.lostcities.worldgen.IDimensionInfo;
import net.dries007.tfc.common.blockentities.LampBlockEntity;
import net.dries007.tfc.common.blocks.devices.LampBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = ChunkFixer.class, remap = false)
public abstract class ChunkFixer_PostProcessSpecialBEMixin {

    @Inject(
            method = "fix",
            at = @At("TAIL")
    )
    private static void tfclc$postProcessSpecialBlockEntities(IDimensionInfo info, ChunkCoord coord, CallbackInfo ci) {
        LevelAccessor level = info.getWorld();
        ChunkPos chunkPos = new ChunkPos(coord.chunkX(), coord.chunkZ());
        Map<BlockPos, PendingBlockEntityKind> pending = LostCitiesPostProcessTracker.consumeChunk(chunkPos);
        if (pending.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, PendingBlockEntityKind> entry : pending.entrySet()) {
            BlockPos pos = entry.getKey();
            PendingBlockEntityKind kind = entry.getValue();


            switch (kind) {
                case LOOT_CONTAINER -> {
                    if (info.getProfile().GENERATE_LOOT && !BuildingInfo.getBuildingInfo(coord, info).noLoot) {
                        LostCitiesLoot.scheduleContainer(level, pos);
                    }
                }
                case FILLED_BOOKSHELF -> tfclc$processBookshelf(level, pos);
                case LAVA_LAMP -> tfclc$processLamp(level, pos);
            }
        }
    }

    @Unique
    private static void tfclc$processBookshelf(LevelAccessor level, BlockPos pos) {
        LostCitiesBookshelves.initialize(level, pos);
    }

    @Unique
    private static void tfclc$processLamp(LevelAccessor level, BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof LampBlockEntity lamp){
            lamp.getCapability(Capabilities.FLUID).ifPresent(fluidHandler -> {
                fluidHandler.fill(new FluidStack(Fluids.LAVA, 250), IFluidHandler.FluidAction.EXECUTE);
                level.setBlock(pos, level.getBlockState(pos).setValue(LampBlock.LIT, true), 2);
            });
        }
    }
}
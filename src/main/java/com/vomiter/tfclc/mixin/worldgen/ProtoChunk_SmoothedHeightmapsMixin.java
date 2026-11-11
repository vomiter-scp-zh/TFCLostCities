package com.vomiter.tfclc.mixin.worldgen;

import com.vomiter.tfclc.mixin.ChunkAccessAccessor;
import com.vomiter.tfclc.worldgen.CityChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunk_SmoothedHeightmapsMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void tfclc$updateWorldgenHeightmaps(
            BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> cir
    ) {
        ProtoChunk chunk = (ProtoChunk) (Object) this;
        CityChunkData data = (CityChunkData) this;
        if (chunk.isOutsideBuildHeight(pos) || cir.getReturnValue() == null
                || (!data.tfclc$isCity() && !data.tfclc$terrainSmoothed())) {
            return;
        }

        // ProtoChunk normally updates the maps for its current status. TFC
        // features still read the WG maps after carving and city placement.
        // Keep existing WG maps current after subsequent feature writes too.
        Map<Heightmap.Types, Heightmap> maps = ((ChunkAccessAccessor) this).tfclc$getHeightmaps();
        Heightmap surface = maps.get(Heightmap.Types.WORLD_SURFACE_WG);
        Heightmap floor = maps.get(Heightmap.Types.OCEAN_FLOOR_WG);
        int x = pos.getX() & 15;
        int z = pos.getZ() & 15;
        if (surface != null) {
            surface.update(x, pos.getY(), z, state);
        }
        if (floor != null) {
            floor.update(x, pos.getY(), z, state);
        }
    }
}

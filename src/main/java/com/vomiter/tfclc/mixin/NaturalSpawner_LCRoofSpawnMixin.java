package com.vomiter.tfclc.mixin;

import com.vomiter.tfclc.util.LostCitiesSpawnHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawner_LCRoofSpawnMixin {

    @Shadow
    private static boolean isValidSpawnPostitionForType(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        MobSpawnSettings.SpawnerData spawnerData,
        BlockPos.MutableBlockPos pos,
        double distanceSq
    ) {
        throw new AssertionError();
    }

    @Redirect(
        method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z"
        )
    )
    private static boolean tfclc$denyRoofNaturalSpawn(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        MobSpawnSettings.SpawnerData spawnerData,
        BlockPos.MutableBlockPos pos,
        double distanceSq
    ) {
        boolean vanilla = isValidSpawnPostitionForType(
            level, category, structureManager, chunkGenerator, spawnerData, pos, distanceSq
        );

        if (!vanilla) {
            return false;
        }

        if (LostCitiesSpawnHelper.shouldDenyNaturalSpawn(level, pos, spawnerData.type)) {
            return false;
        }

        return true;
    }
}
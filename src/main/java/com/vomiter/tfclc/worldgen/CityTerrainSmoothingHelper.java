package com.vomiter.tfclc.worldgen;

import com.vomiter.tfclc.common.registry.TFCLCBlocks;
import com.vomiter.tfclc.mixin.ChunkAccessAccessor;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class CityTerrainSmoothingHelper {
    /**
     * Lost Cities 一層樓的高度。
     * 每向外跨一個 chunk，地形高度最多增加六格。
     */
    public static final int HEIGHT_PER_CHUNK = 3;

    /**
     * 最遠搜尋城市的距離。
     * 12 個 chunk 對應最多 72 格高度差。
     */
    public static final int MAX_SEARCH_RADIUS = 24;

    /**
     * 下移地表時保留的表層厚度。
     */
    private static final int SURFACE_DEPTH = 6;

    private static final EnumSet<Heightmap.Types> ALL_HEIGHTMAPS =
            EnumSet.of(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    Heightmap.Types.OCEAN_FLOOR_WG,
                    Heightmap.Types.WORLD_SURFACE,
                    Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
            );

    public static void rebuildAllHeightmaps(ChunkAccess chunk) {
        Map<Heightmap.Types, Heightmap> heightmaps =
                ((ChunkAccessAccessor) chunk).tfclc$getHeightmaps();

        for (Heightmap.Types type : ALL_HEIGHTMAPS) {
            heightmaps.remove(type);
        }

        Heightmap.primeHeightmaps(
                chunk,
                ALL_HEIGHTMAPS
        );
    }

    /**
     * 計算目前 normal chunk 的地形高度上限。
     * 如果目前是城市、附近沒有城市、自然地形已經能正常銜接，
     * 或中途已經接回自然地形，便回傳 empty。
     */
    public static OptionalInt findTargetHeight(
            IDimensionInfo provider,
            TFCChunkGenerator generator,
            ChunkPos currentPos
    ) {
        BuildingInfo currentInfo =
                getBuildingInfo(provider, currentPos);

        if (isGeneratedAsCity(currentInfo)) {
            return OptionalInt.empty();
        }

        Map<Long, Integer> naturalHeightCache =
                new HashMap<>();

        int currentNaturalHeight = getNaturalHeight(
                generator,
                currentPos,
                naturalHeightCache
        );

        List<CitySeed> nearestCities = findNearestCities(provider, currentPos, MAX_SEARCH_RADIUS);

        if (nearestCities.isEmpty()) { //無城市
            return OptionalInt.empty();
        }

        int bestTarget = Integer.MAX_VALUE; //從最高往下追

        for (CitySeed city : nearestCities) {
            //每個city進行一次計算
            int distance = chebyshevDistance(
                    city.pos(),
                    currentPos
            );

            int allowedHeight = city.cityGround() + HEIGHT_PER_CHUNK * (distance - 1);
            //每隔距離允許增加 6 格高度

             //目前自然高度已經低於坡度上限 = 目前 chunk 本身就是自然銜接點。
            if (currentNaturalHeight <= allowedHeight) {
                continue;
            }

            /*
             * 如果從城市到目前 chunk 的途中已經接回自然
             * 就不應再次讓同一座城市影響目前 chunk。
             */
            if (hasNaturalJoinBefore(
                    generator,
                    city,
                    currentPos,
                    naturalHeightCache
            )) {
                continue;
            }

            bestTarget = Math.min(bestTarget, allowedHeight);
        }

        if (bestTarget == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(bestTarget);
    }

    /**
     * 找出距離目前 chunk 最近的 city chunk
     * 會把有相同距離的chunk都包在list內回傳
     */
    private static List<CitySeed> findNearestCities(
            IDimensionInfo provider,
            ChunkPos origin,
            int maxRadius
    ) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            List<CitySeed> found = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    ChunkPos candidatePos = new ChunkPos(origin.x + dx, origin.z + dz);
                    BuildingInfo info = getBuildingInfo(provider, candidatePos);
                    if (isGeneratedAsCity(info)) {
                        found.add(
                                new CitySeed(
                                        candidatePos,
                                        info.getCityGroundLevel()
                                )
                        );
                    }
                }
            }

            if (!found.isEmpty()) {
                return found;
            }
        }

        return List.of();
    }

    private static BuildingInfo getBuildingInfo(
            IDimensionInfo provider,
            ChunkPos pos
    ) {
        ChunkCoord coord = new ChunkCoord(
                provider.getType(),
                pos.x,
                pos.z
        );

        return BuildingInfo.getBuildingInfo(coord, provider);
    }

    /**
     * same as the condition in LC generate()
     */
    public static boolean isGeneratedAsCity(
            BuildingInfo info
    ) {
        return info.isCity || (info.outsideChunk && info.hasBuilding);
    }

    /**
     * 檢查城市與目前 chunk 之間，是否已經提前接回自然地形。
     * Chebyshev 距離的離散直線
     */
    private static boolean hasNaturalJoinBefore(
            TFCChunkGenerator generator,
            CitySeed city,
            ChunkPos destination,
            Map<Long, Integer> naturalHeightCache
    ) {
        int deltaX = destination.x - city.pos().x;

        int deltaZ = destination.z - city.pos().z;

        int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        //相鄰不會有中間點
        if (steps <= 1) {
            return false;
        }

        ChunkPos previous = city.pos();

        for (int step = 1; step < steps; step++) {

            double progress = step / (double) steps;
            int chunkX = city.pos().x + (int) Math.round(deltaX * progress);
            int chunkZ = city.pos().z + (int) Math.round(deltaZ * progress);
            ChunkPos samplePos = new ChunkPos(chunkX, chunkZ);
            /*
            * 例子    :
            * 城市 chunk = (10, 20)
            * 目標 chunk = (14, 22)
            * cityGround = 71
            * 如果允許斜向移動，從 (10,20) 到 (14,22) 需要四步
            * => (10, 20), (11, 21), (12, 21), (13, 21), (14, 22)
            * https://en.wikipedia.org/wiki/Chebyshev_distance
             */

            // 避免離散化時重複取到同一個 chunk
            if (samplePos.equals(previous)) {
                continue;
            }

            previous = samplePos;

            int distance = chebyshevDistance(city.pos(), samplePos);

            int allowedHeight = city.cityGround() + HEIGHT_PER_CHUNK * (distance - 1);

            int naturalHeight = getNaturalHeight(generator, samplePos, naturalHeightCache);

            if (naturalHeight <= allowedHeight) {
                return true;
            }
        }

        return false;
    }

    /**
     * 取得 TFC 自然地形代表高度。
     * 使用中心和四角共五個取樣點，取最大值
     */
    public static int getNaturalHeight(TFCChunkGenerator generator, ChunkPos chunkPos) {
        return getNaturalHeight(generator, chunkPos, new HashMap<>());
    }

    private static int getNaturalHeight(TFCChunkGenerator generator, ChunkPos chunkPos, Map<Long, Integer> cache) {
        long key = chunkPos.toLong();

        Integer cached = cache.get(key);
        if (cached != null) { return cached; }

        ChunkHeightFiller filler = generator.createHeightFillerForChunk(chunkPos);

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        int center = sample(filler, minX + 8, minZ + 8);
        int northWest = sample(filler, minX + 2, minZ + 2);
        int northEast = sample(filler, minX + 14, minZ + 2);
        int southWest = sample(filler, minX + 2, minZ + 14);
        int southEast = sample(filler, minX + 14, minZ + 14);
        int height = Math.max(
                center,
                Math.max(
                        Math.max(northWest, northEast),
                        Math.max(southWest, southEast)
                )
        );

        cache.put(key, height);
        return height;
    }

    private static int sample(ChunkHeightFiller filler, int blockX, int blockZ) {
        return (int) Math.floor(filler.sampleHeight(blockX, blockZ));
    }

    /**
     * 只削低目前 chunk 中高於 targetHeight 的地表。
     * 每欄保留最上方六層，搬到新的高度，避免把所有地表都直接
     * 變成裸岩。
     */
    public static boolean lowerCurrentChunk(ChunkAccess chunk, int targetHeight) {
        int minBuildHeight = chunk.getMinBuildHeight();
        int maxBuildHeight = chunk.getMaxBuildHeight();
        targetHeight = Math.max(
                minBuildHeight + SURFACE_DEPTH,
                Math.min(targetHeight, maxBuildHeight - 1)
        );

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ);

                if (surfaceY <= targetHeight) {
                    continue;
                }

                int x = minX + localX;
                int z = minZ + localZ;

                int availableDepth = Math.min(SURFACE_DEPTH, surfaceY - minBuildHeight + 1);

                BlockState[] surface = new BlockState[availableDepth];
                /*
                 * 保存原始地表，由最上層往下。
                 */
                for (int depth = 0; depth < availableDepth; depth++) {
                    cursor.set(x, surfaceY - depth, z);
                    surface[depth] = chunk.getBlockState(cursor);
                }

                for (int y = targetHeight; y <= surfaceY; y++) {
                    cursor.set(x, y, z);
                    chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                }

                for (int depth = 0; depth < availableDepth; depth++) {

                    int y = targetHeight - depth;
                    if (y < minBuildHeight) {
                        break;
                    }

                    cursor.set(x, y, z);
                    chunk.setBlockState(cursor, surface[depth], false);

                }

                changed = true;
            }
        }

        return changed;
    }

    public static int chebyshevDistance(ChunkPos first, ChunkPos second) {
        return Math.max(
                Math.abs(first.x - second.x),
                Math.abs(first.z - second.z)
        );
    }

    private record CitySeed(ChunkPos pos, int cityGround) {
    }

    public static void primeAllHeightmaps(
            ChunkAccess chunk
    ) {
        Heightmap.primeHeightmaps(
                chunk,
                ALL_HEIGHTMAPS
        );
    }
}
package com.vomiter.tfclc.worldgen;

import com.vomiter.tfclc.Helpers;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.GroundcoverBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class CityTerrainSmoothingHelper {

    public static final int HEIGHT_PER_CHUNK = 3;

    public static final int MAX_SEARCH_RADIUS = 24;

    private static final int SURFACE_DEPTH = 6;
    private static final TagKey<Biome> SHOULD_SMOOTH = TagKey.create(Registries.BIOME, Helpers.id("should_smooth"));

    private static final BlockState GRASS = TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.ENTISOL).get().defaultBlockState();

    private static final EnumSet<Heightmap.Types> WORLDGEN_HEIGHTMAPS =
            EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG);

    private static final EnumSet<Heightmap.Types> ALL_HEIGHTMAPS =
            EnumSet.of(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    Heightmap.Types.OCEAN_FLOOR_WG,
                    Heightmap.Types.WORLD_SURFACE,
                    Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
            );

    public static void smoothChunk(
            WorldGenRegion level, IDimensionInfo provider, TFCChunkGenerator generator, ChunkAccess chunk
    ) {
        ChunkPos pos = chunk.getPos();
        BuildingInfo info = getBuildingInfo(provider, pos);
        CityChunkData chunkData = (CityChunkData) chunk;

        if (isGeneratedAsCity(info)) {
            int floor = info.getCityGroundLevel();
            chunkData.tfclc$setCityFloor(floor);
            HeightSample city = HeightSample.full(floor);
            lowerCurrentChunk(level, chunk, new SmoothingProfile(city, city, city, city, city));
            return;
        }

        // A corner always uses the same four chunk centers, in the same order.
        Map<Long, Integer> naturalHeightCache = new HashMap<>();
        HeightSample[][] samples = new HeightSample[3][3];
        for (int z = 0; z < 3; z++) {
            for (int x = 0; x < 3; x++) {
                if (x == 1 && z == 1) {
                    samples[z][x] = sampleCenter(level, provider, generator, chunk, info, naturalHeightCache);
                } else {
                    ChunkAccess neighbor = level.getChunk(pos.x + x - 1, pos.z + z - 1);
                    samples[z][x] = sampleCenter(level, provider, generator, neighbor,
                            getBuildingInfo(provider, neighbor.getPos()), naturalHeightCache);
                }
            }
        }

        SmoothingProfile profile = new SmoothingProfile(
                samples[1][1],
                corner(samples, 0, 0), corner(samples, 1, 0),
                corner(samples, 0, 1), corner(samples, 1, 1)
        );
        if (profile.hasCut() && lowerCurrentChunk(level, chunk, profile)) {
            chunkData.tfclc$setTerrainSmoothed(true);
        }
    }

    private static HeightSample sampleCenter(
            WorldGenRegion level, IDimensionInfo provider, TFCChunkGenerator generator,
            ChunkAccess chunk, BuildingInfo info, Map<Long, Integer> naturalHeightCache
    ) {
        int floor = info.getCityGroundLevel();
        if (isGeneratedAsCity(info)) {
            return new HeightSample(floor, 1, true);
        }

        ChunkPos pos = chunk.getPos();
        if (!chunk.getNoiseBiome(
                QuartPos.fromBlock(pos.getMiddleBlockX()),
                QuartPos.fromBlock(floor),
                QuartPos.fromBlock(pos.getMiddleBlockZ())
        ).is(SHOULD_SMOOTH)) {
            return HeightSample.NONE;
        }

        OptionalInt target = findTargetHeight(level, provider, generator, pos, naturalHeightCache);
        return target.isPresent() ? HeightSample.full(target.getAsInt()) : HeightSample.NONE;
    }

    private static HeightSample corner(HeightSample[][] samples, int x, int z) {
        double cityFloor = Double.POSITIVE_INFINITY;
        double heightSum = 0;
        double strengthSum = 0;
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                HeightSample sample = samples[z + dz][x + dx];
                if (sample.city()) {
                    cityFloor = Math.min(cityFloor, sample.weightedHeight());
                }
                heightSum += sample.weightedHeight();
                strengthSum += sample.strength();
            }
        }

        // Keep the transition anchored to the city footprint on shared edges.
        if (cityFloor != Double.POSITIVE_INFINITY) {
            return HeightSample.full(cityFloor);
        }
        return new HeightSample(heightSum / 4, strengthSum / 4, false);
    }

    private static OptionalInt findTargetHeight(
            WorldGenRegion level, IDimensionInfo provider, TFCChunkGenerator generator,
            ChunkPos currentPos, Map<Long, Integer> naturalHeightCache
    ) {
        List<CitySeed> nearestCities = findNearestCities(level, provider, currentPos, MAX_SEARCH_RADIUS);
        if (nearestCities.isEmpty()) {
            return OptionalInt.empty();
        }

        int currentNaturalHeight = getNaturalHeight(generator, currentPos, naturalHeightCache);
        int bestTarget = Integer.MAX_VALUE; //從最高往下追

        for (CitySeed city : nearestCities) {
            //每個city進行一次計算
            int distance = chebyshevDistance(
                    city.pos(),
                    currentPos
            );

            int allowedHeight = city.cityGround() + HEIGHT_PER_CHUNK * (distance - 1);

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
            WorldGenRegion level,
            IDimensionInfo provider,
            ChunkPos origin,
            int maxRadius
    ) {
        CityChunkData originData = (CityChunkData) level.getChunk(origin.x, origin.z);
        int cachedDistance = originData.tfclc$getDistanceToNearestCities();
        if (cachedDistance > maxRadius) {
            return List.of();
        }

        int minSearchRadius = 1;
        int maxSearchRadius = maxRadius;
        if (cachedDistance > 0) {
            minSearchRadius = cachedDistance;
            maxSearchRadius = cachedDistance;
        } else {
            CitySearchCache cache = getNearestCitySearchCache(level, origin, maxRadius);
            if (cache != null) {
                minSearchRadius = Math.max(1, cache.distanceToNearestCities - cache.distanceBetweenTwoChunks);
                maxSearchRadius = Math.min(maxRadius, cache.distanceToNearestCities + cache.distanceBetweenTwoChunks);
            }
        }

        for (int radius = minSearchRadius; radius <= maxSearchRadius; radius++) {
            List<CitySeed> found = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    ChunkPos candidatePos = new ChunkPos(origin.x + dx, origin.z + dz);
                    BuildingInfo info = getBuildingInfo(provider, candidatePos);
                    if (isGeneratedAsCity(info)) {
                        found.add(new CitySeed(candidatePos, info.getCityGroundLevel()));
                    }
                }
            }

            if (!found.isEmpty()) {
                originData.tfclc$setDistanceToNearestCities(radius);
                return found;
            }
        }
        originData.tfclc$setDistanceToNearestCities(maxRadius + 1);
        return List.of();
    }


    /**
     * 根據附近能取得的 Chunk Access 決定之後搜尋的 radius 上下界
     */
    @Nullable
    private static CitySearchCache getNearestCitySearchCache(WorldGenRegion level, ChunkPos origin, int maxRadius){
        int minCacheDistance = Integer.MAX_VALUE;
        boolean found = false;
        for (int radius = 1; radius <= maxRadius / 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    ChunkPos candidatePos = new ChunkPos(origin.x + dx, origin.z + dz);
                    if(level.hasChunk(candidatePos.x, candidatePos.z)){
                        if (level.getChunk(candidatePos.x, candidatePos.z) instanceof CityChunkData chunkData){
                            if (chunkData.tfclc$getDistanceToNearestCities() == -1){
                                continue;
                            }
                            minCacheDistance = Math.min(minCacheDistance, chunkData.tfclc$getDistanceToNearestCities());
                            found = true;
                        }
                    }
                }
            }

            if (found) {
                return new CitySearchCache(radius, minCacheDistance);
            }
        }
        return null;
    }



    record CitySearchCache(int distanceBetweenTwoChunks, int distanceToNearestCities){}

    private static BuildingInfo getBuildingInfo(
            IDimensionInfo provider,
            ChunkPos pos
    ) {
        ChunkCoord coord = new ChunkCoord(provider.getType(), pos.x, pos.z);
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
     * 取得 TFC 自然地形代表高度。使用中心和四角共五個取樣點，取最大值
     */
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
    private static boolean lowerCurrentChunk(WorldGenRegion level, ChunkAccess chunk, SmoothingProfile profile) {
        int minBuildHeight = chunk.getMinBuildHeight();
        int maxBuildHeight = chunk.getMaxBuildHeight();

        // At CARVERS/FEATURES, ProtoChunk updates only the final heightmaps.
        // Refresh the WG maps before using them to locate the original ground.
        Heightmap.primeHeightmaps(chunk, WORLDGEN_HEIGHTMAPS);

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {

                int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ);

                int x = minX + localX;
                int z = minZ + localZ;
                // A non-full groundcover may be counted by the floor map.
                // Preserve and move soil, not the decoration sitting on it.
                cursor.set(x, surfaceY, z);
                while (surfaceY > minBuildHeight && isSurfaceCover(chunk.getBlockState(cursor))) {
                    cursor.setY(--surfaceY);
                }

                int localTargetHeight = Math.max(
                        minBuildHeight + SURFACE_DEPTH,
                        profile.heightLimit(surfaceY, localX, localZ)
                );

                if (surfaceY <= localTargetHeight) {
                    continue;
                }

                int availableDepth = Math.min(SURFACE_DEPTH, surfaceY - minBuildHeight + 1);

                BlockState[] surface = new BlockState[availableDepth];

                for (int depth = 0; depth < availableDepth; depth++) {
                    cursor.set(x, surfaceY - depth, z);
                    surface[depth] = chunk.getBlockState(cursor);
                }

                // FEATURES can spill into this chunk before its own smoothing.
                // OCEAN_FLOOR_WG excludes fluid and groundcover, so clear those
                // above the old ground as well, including gaps between them.
                int oldTop = Math.min(maxBuildHeight - 1,
                        chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ));
                cursor.set(x, surfaceY + 1, z);
                BlockState groundcover = chunk.getBlockState(cursor);
                for (int y = surfaceY + 1; y <= oldTop; y++) {
                    cursor.set(x, y, z);
                    BlockState decoration = chunk.getBlockState(cursor);
                    if (isSurfaceCover(decoration)) {
                        chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                    }
                }

                for (int y = localTargetHeight; y <= surfaceY; y++) {
                    cursor.set(x, y, z);
                    chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                }

                for (int depth = 0; depth < availableDepth; depth++) {

                    int y = localTargetHeight - depth;
                    if (y < minBuildHeight) {
                        break;
                    }

                    cursor.set(x, y, z);
                    if (depth == 0 && !surface[depth].is(BlockTags.DIRT)){
                        chunk.setBlockState(cursor, GRASS, false);
                    } else {
                        chunk.setBlockState(cursor, surface[depth], false);
                    }

                }

                // Preserve existing sticks/rocks on the new dry surface. Do not
                // bring the fluid from a submerged groundcover down with it.
                if (groundcover.getBlock() instanceof GroundcoverBlock) {
                    BlockState relocated = FluidHelpers.emptyFluidFrom(groundcover);
                    cursor.set(x, localTargetHeight + 1, z);
                    if (chunk.getBlockState(cursor).isAir() && relocated.canSurvive(level, cursor)) {
                        chunk.setBlockState(cursor, relocated, false);
                    }
                }

                changed = true;
            }
        }

        if (changed) {
            // TFC plants still query WORLD_SURFACE_WG during decoration. Refresh
            // every map here so city-only paths and early returns are covered too.
            primeAllHeightmaps(chunk);
        }
        return changed;
    }

    private static boolean isSurfaceCover(BlockState state) {
        return state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof GroundcoverBlock
                || state.getBlock() instanceof LiquidBlock
                || state.getBlock() instanceof BubbleColumnBlock;
    }

    private record HeightSample(double weightedHeight, double strength, boolean city) {
        private static final HeightSample NONE = new HeightSample(0, 0, false);

        private static HeightSample full(double height) {
            return new HeightSample(height, 1, false);
        }
    }

    private record SmoothingProfile(
            HeightSample center, HeightSample northWest, HeightSample northEast,
            HeightSample southWest, HeightSample southEast
    ) {
        private boolean hasCut() {
            return center.strength() > 0
                    || northWest.strength() > 0 || northEast.strength() > 0
                    || southWest.strength() > 0 || southEast.strength() > 0;
        }

        private int heightLimit(int surfaceY, int localX, int localZ) {
            double x = localX / 15.0;
            double z = localZ / 15.0;
            // The outer columns use only the shared edge. Toward the center,
            // blend back to this chunk's own plan (which may have no cut).
            double edgeDistance = Math.max(Math.abs(2 * x - 1), Math.abs(2 * z - 1));
            double edgeWeight = edgeDistance * edgeDistance * (3 - 2 * edgeDistance);
            double edgeHeight = bilerp(
                    northWest.weightedHeight(), northEast.weightedHeight(),
                    southWest.weightedHeight(), southEast.weightedHeight(), x, z
            );
            double edgeStrength = bilerp(
                    northWest.strength(), northEast.strength(),
                    southWest.strength(), southEast.strength(), x, z
            );
            double height = lerp(center.weightedHeight(), edgeHeight, edgeWeight);
            double strength = lerp(center.strength(), edgeStrength, edgeWeight);
            double cut = Math.max(0, surfaceY * strength - height);
            return surfaceY - (int) Math.round(cut);
        }

        private static double bilerp(double nw, double ne, double sw, double se, double x, double z) {
            return lerp(lerp(nw, ne, x), lerp(sw, se, x), z);
        }

        private static double lerp(double from, double to, double weight) {
            return (1 - weight) * from + weight * to;
        }
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

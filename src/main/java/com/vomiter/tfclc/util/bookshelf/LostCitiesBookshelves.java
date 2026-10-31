package com.vomiter.tfclc.util.bookshelf;

import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.dries007.tfc.common.blocks.wood.BookshelfBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LostCitiesBookshelves {
    public static final String INITIALIZED = "tfclc_bookshelf_initialized";

    private LostCitiesBookshelves() {}

    public static void initialize(LevelAccessor level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof BookshelfBlock)) return;
        if (!(getOrCreate(level, pos) instanceof ChiseledBookShelfBlockEntity shelf)) return;
        if (shelf.getLevel() instanceof ServerLevel server) {
            BlockPos target = pos.immutable();
            // World generation may be visiting a chunk that is already live.
            server.getServer().execute(() -> {
                if (!shelf.isRemoved() && server.getBlockEntity(target) == shelf) {
                    fill(server, target, shelf);
                }
            });
        } else {
            // Bookshelf.load only reads inventory NBT; it has no live-level callbacks.
            fill(level, pos, shelf);
        }
    }

    @Nullable
    private static BlockEntity getOrCreate(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof WorldGenRegion)) return level.getBlockEntity(pos);
        ChunkAccess chunk = level.getChunk(pos);
        BlockEntity entity = chunk.getBlockEntity(pos);
        // TFC's subclass uses the vanilla type in its constructor. Saved shelves can
        // consequently reload as the vanilla base class. Both implement the same inventory.
        if (entity instanceof ChiseledBookShelfBlockEntity) return entity;
        CompoundTag saved = chunk.getBlockEntityNbt(pos);
        if (saved != null && !"DUMMY".equals(saved.getString("id"))) {
            entity = BlockEntity.loadStatic(pos, chunk.getBlockState(pos), saved, level.registryAccess());
        }
        if (!(entity instanceof ChiseledBookShelfBlockEntity)) {
            entity = ((EntityBlock) chunk.getBlockState(pos).getBlock()).newBlockEntity(pos, chunk.getBlockState(pos));
        }
        if (entity instanceof ChiseledBookShelfBlockEntity) {
            chunk.removeBlockEntity(pos);
            chunk.setBlockEntity(entity);
            chunk.setUnsaved(true);
        }
        return entity;
    }

    private static void fill(LevelAccessor level, BlockPos pos, ChiseledBookShelfBlockEntity shelf) {
        if (shelf.getPersistentData().getBoolean(INITIALIZED)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BookshelfBlock)) return;
        BlockState result = LostCitiesBookshelfFacingHelper.fixFacing(level, pos, state);
        if (shelf.isEmpty()) {
            long seed = (level instanceof ServerLevel server ? server.getSeed()
                    : level instanceof WorldGenRegion region ? region.getSeed() : 0L)
                    ^ Mth.getSeed(pos) ^ 0x6E624EB7D4C15A3BL;
            result = LostCitiesBookshelfFiller.fillRandom(shelf, result, RandomSource.create(seed), level.registryAccess());
        } else {
            // Never overwrite an existing inventory when processing a saved entity.
            for (int i = 0; i < 6; i++) {
                result = result.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i),
                        !shelf.getItem(i).isEmpty());
            }
        }
        if (result != state && !level.setBlock(pos, result, 2)) return;
        shelf.getPersistentData().putBoolean(INITIALIZED, true);
        shelf.setChanged();
    }
}

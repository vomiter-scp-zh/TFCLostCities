package com.vomiter.tfclc.util.bookshelf;

import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

public final class LostCitiesBookshelfFiller {

    private LostCitiesBookshelfFiller() {
    }

    public static BlockState fillRandom(ChiseledBookShelfBlockEntity bookshelf, BlockState state, RandomSource random, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);

        for (int i = 0; i < 6; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (random.nextFloat() < 0.65f) {
                stack = randomBook(random);
            }
            items.set(i, stack);

            // 一次性同步 slot occupied 屬性，不走 setItem()
            state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), !stack.isEmpty());
        }

        // Preserve custom names, Forge data and other mods' saved fields.
        CompoundTag tag = bookshelf.saveWithoutMetadata(registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        bookshelf.loadWithComponents(tag, registries);
        bookshelf.setChanged();

        return state;
    }

    private static ItemStack randomBook(RandomSource random) {
        float roll = random.nextFloat();

        if (roll < 0.75f) {
            return new ItemStack(Items.BOOK);
        }
        return new ItemStack(Items.WRITABLE_BOOK);
    }
}
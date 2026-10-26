package com.vomiter.tfclc.data.loot;

import com.vomiter.tfclc.util.loot.LostCitiesLoot;
import com.vomiter.tfclc.util.loot.BarrelContentFunction;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.common.fluids.SimpleFluid;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.items.Food;
import net.dries007.tfc.util.Metal;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class SubLootTable implements LootTableSubProvider {
    @Override
    public void generate(@NotNull BiConsumer<ResourceLocation, LootTable.Builder> output) {
        LootPool.Builder tools = LootPool.lootPool().setRolls(UniformGenerator.between(1, 3));
        for (Metal.Default metal : new Metal.Default[] {Metal.Default.COPPER, Metal.Default.BRONZE, Metal.Default.WROUGHT_IRON}) {
            for (Metal.ItemType type : new Metal.ItemType[] {Metal.ItemType.PICKAXE, Metal.ItemType.AXE, Metal.ItemType.SHOVEL, Metal.ItemType.HOE, Metal.ItemType.KNIFE, Metal.ItemType.HAMMER, Metal.ItemType.SAW}) {
                tools.add(LootItem.lootTableItem(TFCItems.METAL_ITEMS.get(metal).get(type).get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.05f, 0.8f))));
            }
        }
        output.accept(LostCitiesLoot.TOOL_RACK, LootTable.lootTable().withPool(tools));
        output.accept(LostCitiesLoot.BARREL, LootTable.lootTable().withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(TFCItems.VESSEL.get()).setWeight(2)
                        .apply(BarrelContentFunction.content(ForgeMod.MILK.get(), 4000, false, true)))
                .add(LootItem.lootTableItem(TFCItems.VESSEL.get()).setWeight(3)
                        .apply(BarrelContentFunction.content(Fluids.WATER, 4000, false, true)))
                .add(LootItem.lootTableItem(TFCItems.VESSEL.get()).setWeight(2)
                        .apply(BarrelContentFunction.content(TFCFluids.SIMPLE_FLUIDS.get(SimpleFluid.VINEGAR).getSource(), 4000, false, true)))
                .add(LootItem.lootTableItem(TFCItems.VESSEL.get())
                        .apply(BarrelContentFunction.content(TFCFluids.SIMPLE_FLUIDS.get(SimpleFluid.BRINE).getSource(), 2000, false, true)))
                .add(counted(TFCItems.FOOD.get(Food.CARROT).get(), 4, 12).setWeight(2)
                        .apply(BarrelContentFunction.content(null, 0, true, false)))
                .add(counted(TFCItems.FOOD.get(Food.CABBAGE).get(), 4, 12).setWeight(2)
                        .apply(BarrelContentFunction.content(TFCFluids.SIMPLE_FLUIDS.get(SimpleFluid.VINEGAR).getSource(), 3000, true, false)))
                .add(counted(TFCItems.FOOD.get(Food.COOKED_PORK).get(), 4, 12).setWeight(2)
                        .apply(BarrelContentFunction.content(TFCFluids.SIMPLE_FLUIDS.get(SimpleFluid.VINEGAR).getSource(), 3000, true, false)))
        ));
        var chest = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(3, 6));
        TFCItems.CROP_SEEDS.values().forEach(seed -> chest.add(counted(seed.get(), 2, 16)));
        chest.add(counted(TFCItems.JUTE_FIBER.get(), 2, 8));
        chest.add(counted(Items.CLAY_BALL, 4, 32));
        output.accept(LostCitiesLoot.CHEST, LootTable.lootTable().withPool(chest));
        output.accept(LostCitiesLoot.LARGE_VESSEL, LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(UniformGenerator.between(2, 5))
                .add(counted(TFCItems.FOOD.get(Food.WHEAT_GRAIN).get(), 4, 12))
                .add(counted(TFCItems.FOOD.get(Food.RICE_GRAIN).get(), 4, 12))
                .add(counted(TFCItems.FOOD.get(Food.MAIZE_GRAIN).get(), 4, 12))
                .add(counted(TFCItems.FOOD.get(Food.DRIED_KELP).get(), 2, 8))));
        output.accept(LostCitiesLoot.BASEMENT_INGOTS, ingots(5, 24, Metal.Default.COPPER, Metal.Default.TIN, Metal.Default.ZINC, Metal.Default.WROUGHT_IRON));
        output.accept(LostCitiesLoot.RAIL_INGOTS, ingots(6, 24, Metal.Default.NICKEL, Metal.Default.BISMUTH, Metal.Default.GOLD, Metal.Default.SILVER));
        output.accept(LostCitiesLoot.SMALL_VESSEL, LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1, 4))
                        .add(counted(TFCItems.FOOD.get(Food.WHEAT_GRAIN).get(), 2, 8))
                        .add(counted(TFCItems.FOOD.get(Food.RICE_GRAIN).get(), 2, 8))
                        .add(counted(TFCItems.FOOD.get(Food.DRIED_KELP).get(), 2, 6))
                        .add(counted(TFCItems.JUTE_FIBER.get(), 1, 4))));
    }

    private static LootTable.Builder ingots(int min, int max, Metal.Default... metals) {
        LootPool.Builder pool = LootPool.lootPool();
        for (Metal.Default metal : metals) {
            pool.add(counted(TFCItems.METAL_ITEMS.get(metal).get(Metal.ItemType.INGOT).get(), min, max).setWeight(4));
        }
        pool.add(counted(TFCItems.METAL_ITEMS.get(Metal.Default.UNKNOWN).get(Metal.ItemType.INGOT).get(), min, max).setWeight(3));
        return LootTable.lootTable().withPool(pool);
    }

    private static LootPoolSingletonContainer.Builder<?> counted(Item item, int min, int max) {
        return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}

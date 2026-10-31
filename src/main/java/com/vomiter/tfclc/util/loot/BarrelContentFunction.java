package com.vomiter.tfclc.util.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import java.util.Optional;
import com.vomiter.tfclc.common.registry.ModLootFunctions;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTraits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

/** Encodes one barrel loot result: an optional fluid and an optional food stack. */
public class BarrelContentFunction extends LootItemConditionalFunction {
    public static final MapCodec<BarrelContentFunction> CODEC = RecordCodecBuilder.<BarrelContentFunction>mapCodec(instance ->
            commonFields(instance).and(instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(value -> Optional.ofNullable(value.fluid)),
                    Codec.INT.optionalFieldOf("amount", 0).forGetter(value -> value.amount),
                    Codec.BOOL.optionalFieldOf("brined", false).forGetter(value -> value.brined),
                    Codec.BOOL.optionalFieldOf("fluid_only", false).forGetter(value -> value.fluidOnly)
            )).apply(instance, (conditions, fluid, amount, brined, fluidOnly) ->
                    new BarrelContentFunction(conditions, fluid.orElse(null), amount, brined, fluidOnly)))
            .validate(value -> {
                if (value.fluid != null && (!BuiltInRegistries.FLUID.containsKey(value.fluid)
                        || BuiltInRegistries.FLUID.get(value.fluid) == Fluids.EMPTY || value.amount <= 0)) {
                    return DataResult.error(() -> "barrel_content requires a registered fluid and a positive amount");
                }
                if (value.fluidOnly && value.fluid == null) {
                    return DataResult.error(() -> "fluid_only requires a fluid");
                }
                return DataResult.success(value);
            });

    public static final String PAYLOAD = "tfclc_barrel_content";
    @Nullable private final ResourceLocation fluid;
    private final int amount;
    private final boolean brined;
    private final boolean fluidOnly;

    private BarrelContentFunction(List<LootItemCondition> conditions, @Nullable ResourceLocation fluid, int amount, boolean brined, boolean fluidOnly) {
        super(conditions);
        this.fluid = fluid;
        this.amount = amount;
        this.brined = brined;
        this.fluidOnly = fluidOnly;
    }

    public static Builder<?> content(@Nullable Fluid fluid, int amount, boolean brined, boolean fluidOnly) {
        return simpleBuilder(conditions -> new BarrelContentFunction(conditions,
                fluid == null ? null : BuiltInRegistries.FLUID.getKey(fluid), amount, brined, fluidOnly));
    }

    @Override
    public LootItemFunctionType<BarrelContentFunction> getType() {
        return ModLootFunctions.BARREL_CONTENT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (brined) {
            FoodCapability.setCreatedNow(stack);
            FoodCapability.applyTrait(stack, FoodTraits.SALTED);
            FoodCapability.applyTrait(stack, FoodTraits.BRINED);
            FoodCapability.applyTrait(stack, FoodTraits.PICKLED);
        }
        if (fluid != null || fluidOnly) {
            CompoundTag payload = new CompoundTag();
            if (fluid != null) {
                payload.putString("fluid", fluid.toString());
                payload.putInt("amount", amount);
            }
            payload.putBoolean("fluid_only", fluidOnly);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(PAYLOAD, payload));
        }
        return stack;
    }

}

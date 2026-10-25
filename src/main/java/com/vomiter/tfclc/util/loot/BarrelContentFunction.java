package com.vomiter.tfclc.util.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.vomiter.tfclc.common.registry.ModLootFunctions;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTraits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** Encodes one barrel loot result: an optional fluid and an optional food stack. */
public class BarrelContentFunction extends LootItemConditionalFunction {
    public static final String PAYLOAD = "tfclc_barrel_content";
    @Nullable private final ResourceLocation fluid;
    private final int amount;
    private final boolean brined;
    private final boolean fluidOnly;

    private BarrelContentFunction(LootItemCondition[] conditions, @Nullable ResourceLocation fluid, int amount, boolean brined, boolean fluidOnly) {
        super(conditions);
        this.fluid = fluid;
        this.amount = amount;
        this.brined = brined;
        this.fluidOnly = fluidOnly;
    }

    public static Builder<?> content(@Nullable Fluid fluid, int amount, boolean brined, boolean fluidOnly) {
        return simpleBuilder(conditions -> new BarrelContentFunction(conditions,
                fluid == null ? null : ForgeRegistries.FLUIDS.getKey(fluid), amount, brined, fluidOnly));
    }

    @Override
    public LootItemFunctionType getType() {
        return ModLootFunctions.BARREL_CONTENT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (brined) {
            FoodCapability.updateFoodDecayOnCreate(stack);
            FoodCapability.applyTrait(stack, FoodTraits.SALTED);
            FoodCapability.applyTrait(stack, FoodTraits.BRINED);
        }
        if (fluid != null || fluidOnly) {
            CompoundTag payload = new CompoundTag();
            if (fluid != null) {
                payload.putString("fluid", fluid.toString());
                payload.putInt("amount", amount);
            }
            payload.putBoolean("fluid_only", fluidOnly);
            stack.getOrCreateTag().put(PAYLOAD, payload);
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<BarrelContentFunction> {
        @Override
        public void serialize(JsonObject json, BarrelContentFunction function, JsonSerializationContext context) {
            super.serialize(json, function, context);
            if (function.fluid != null) {
                json.addProperty("fluid", function.fluid.toString());
                json.addProperty("amount", function.amount);
            }
            json.addProperty("brined", function.brined);
            json.addProperty("fluid_only", function.fluidOnly);
        }

        @Override
        public BarrelContentFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            ResourceLocation fluid = json.has("fluid") ? ResourceLocation.tryParse(GsonHelper.getAsString(json, "fluid")) : null;
            int amount = GsonHelper.getAsInt(json, "amount", 0);
            boolean fluidOnly = GsonHelper.getAsBoolean(json, "fluid_only", false);
            if (json.has("fluid") && (fluid == null || !ForgeRegistries.FLUIDS.containsKey(fluid) || ForgeRegistries.FLUIDS.getValue(fluid) == Fluids.EMPTY || amount <= 0)) {
                throw new JsonSyntaxException("barrel_content requires a registered fluid and a positive amount");
            }
            if (fluidOnly && fluid == null) {
                throw new JsonSyntaxException("fluid_only requires a fluid");
            }
            return new BarrelContentFunction(conditions, fluid, amount, GsonHelper.getAsBoolean(json, "brined", false), fluidOnly);
        }
    }
}

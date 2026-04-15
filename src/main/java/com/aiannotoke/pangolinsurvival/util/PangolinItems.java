package com.aiannotoke.pangolinsurvival.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

public final class PangolinItems {

    private static final String STARTER_HAT_FLAG = "pangolin_starter_hat";

    private PangolinItems() {
    }

    public static ItemStack createStarterHat() {
        ItemStack stack = new ItemStack(Items.LEATHER_HELMET);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.pangolinsurvival.starter_hat"));
        stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(STARTER_HAT_FLAG, true));
        return stack;
    }

    public static boolean isStarterHat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        NbtCompound nbt = customData.copyNbt();
        return nbt.getBoolean(STARTER_HAT_FLAG, false);
    }
}

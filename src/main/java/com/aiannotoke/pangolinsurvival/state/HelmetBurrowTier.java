package com.aiannotoke.pangolinsurvival.state;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum HelmetBurrowTier {
    NONE("none", null, 0.0F, 999.0F),
    BASIC("basic", Items.STONE_PICKAXE, 1.10F, 1.25F),
    IRON("iron", Items.IRON_PICKAXE, 1.10F, 1.0F),
    DIAMOND("diamond", Items.DIAMOND_PICKAXE, 1.12F, 0.8F),
    NETHERITE("netherite", Items.NETHERITE_PICKAXE, 1.15F, 0.65F);

    private final String id;
    private final Item referencePickaxe;
    private final ItemStack referencePickaxeStack;
    private final float speedBoost;
    private final float staminaMultiplier;

    HelmetBurrowTier(String id, Item referencePickaxe, float speedBoost, float staminaMultiplier) {
        this.id = id;
        this.referencePickaxe = referencePickaxe;
        this.referencePickaxeStack = referencePickaxe == null ? ItemStack.EMPTY : new ItemStack(referencePickaxe);
        this.speedBoost = speedBoost;
        this.staminaMultiplier = staminaMultiplier;
    }

    public String id() {
        return id;
    }

    public float breakingSpeed() {
        return speedBoost;
    }

    public float staminaMultiplier() {
        return staminaMultiplier;
    }

    public boolean canDig() {
        return this != NONE;
    }

    public float effectiveMiningSpeed(BlockState state) {
        if (referencePickaxeStack.isEmpty()) {
            return 0.0F;
        }
        return referencePickaxeStack.getMiningSpeedMultiplier(state) * speedBoost;
    }

    public boolean canHarvest(BlockState state) {
        if (!state.isToolRequired()) {
            return true;
        }
        if (referencePickaxeStack.isEmpty()) {
            return false;
        }
        return referencePickaxeStack.isSuitableFor(state);
    }

    public static HelmetBurrowTier fromHelmet(ItemStack helmet) {
        if (helmet == null || helmet.isEmpty()) {
            return NONE;
        }

        Item item = helmet.getItem();
        if (item == Items.LEATHER_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.GOLDEN_HELMET || item == Items.TURTLE_HELMET) {
            return BASIC;
        }
        if (item == Items.IRON_HELMET) {
            return IRON;
        }
        if (item == Items.DIAMOND_HELMET) {
            return DIAMOND;
        }
        if (item == Items.NETHERITE_HELMET) {
            return NETHERITE;
        }
        return NONE;
    }

    public static HelmetBurrowTier fromId(String id) {
        for (HelmetBurrowTier value : values()) {
            if (value.id.equalsIgnoreCase(id)) {
                return value;
            }
        }
        return NONE;
    }
}

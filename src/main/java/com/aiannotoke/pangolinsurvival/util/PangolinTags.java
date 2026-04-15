package com.aiannotoke.pangolinsurvival.util;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class PangolinTags {

    public static final TagKey<Block> HARD_BURROW_BAN =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of(PangolinSurvivalMod.MOD_ID, "hard_burrow_ban"));

    private PangolinTags() {
    }
}

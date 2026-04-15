package com.aiannotoke.pangolinsurvival.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class PangolinConfig {

    public boolean globalEnabled;
    public int maxStamina;
    public int digEnterCost;
    public float blockBreakStaminaMultiplier;
    public int blockPlaceCost;
    public float staminaRecoveryPerTick;
    public float ghostFlySpeed;
    public float digFlySpeed;
    public Map<String, Integer> playerMaxStamina;

    public boolean fillDefaults() {
        boolean changed = false;
        if (maxStamina <= 0) {
            maxStamina = 100;
            changed = true;
        }
        if (digEnterCost <= 0) {
            digEnterCost = 6;
            changed = true;
        }
        if (blockBreakStaminaMultiplier <= 0.0F) {
            blockBreakStaminaMultiplier = 7.5F;
            changed = true;
        }
        if (blockPlaceCost <= 0) {
            blockPlaceCost = 4;
            changed = true;
        }
        if (staminaRecoveryPerTick <= 0.0F) {
            staminaRecoveryPerTick = 0.2F;
            changed = true;
        }
        if (ghostFlySpeed <= 0.0F) {
            ghostFlySpeed = 0.03F;
            changed = true;
        }
        if (digFlySpeed <= 0.0F) {
            digFlySpeed = 0.02F;
            changed = true;
        }
        if (playerMaxStamina == null) {
            playerMaxStamina = new LinkedHashMap<>();
            changed = true;
        }
        return changed;
    }
}

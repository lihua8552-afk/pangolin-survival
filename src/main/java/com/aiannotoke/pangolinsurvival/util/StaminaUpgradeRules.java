package com.aiannotoke.pangolinsurvival.util;

public final class StaminaUpgradeRules {

    public static final int MAX_STAMINA_CAP = 1000;
    public static final int STAMINA_UPGRADE_STEP = 50;
    public static final int BASE_LEVEL_COST = 3;
    public static final int LEVEL_COST_INCREMENT = 2;

    private StaminaUpgradeRules() {
    }

    public static int normalizeBaseMax(int configuredBaseMax) {
        return Math.max(1, Math.min(MAX_STAMINA_CAP, configuredBaseMax));
    }

    public static int normalizeStoredMax(int configuredBaseMax, int storedMax) {
        int baseMax = normalizeBaseMax(configuredBaseMax);
        return Math.max(baseMax, Math.min(MAX_STAMINA_CAP, storedMax));
    }

    public static boolean isAtCap(int currentMax) {
        return currentMax >= MAX_STAMINA_CAP;
    }

    public static int getNextMaxStamina(int configuredBaseMax, int currentMax) {
        int normalizedCurrent = normalizeStoredMax(configuredBaseMax, currentMax);
        if (isAtCap(normalizedCurrent)) {
            return normalizedCurrent;
        }
        return Math.min(MAX_STAMINA_CAP, normalizedCurrent + STAMINA_UPGRADE_STEP);
    }

    public static int getUpgradeCostLevels(int configuredBaseMax, int currentMax) {
        int baseMax = normalizeBaseMax(configuredBaseMax);
        int normalizedCurrent = normalizeStoredMax(baseMax, currentMax);
        if (normalizedCurrent >= MAX_STAMINA_CAP) {
            return 0;
        }

        int tier = Math.max(0, (normalizedCurrent - baseMax) / STAMINA_UPGRADE_STEP);
        return BASE_LEVEL_COST + tier * LEVEL_COST_INCREMENT;
    }
}

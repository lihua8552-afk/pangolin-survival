package com.aiannotoke.pangolinsurvival.state;

public enum BurrowPhase {
    NONE("none", false, false, false),
    ENTERING_GHOST("entering_ghost", true, true, true),
    GHOST_ACTIVE("ghost_active", true, true, true),
    ENTERING_DIG("entering_dig", true, true, true),
    DIG_ACTIVE("dig_active", true, true, true),
    EXITING("exiting", true, false, true);

    private final String id;
    private final boolean burrowing;
    private final boolean spectatorMotionEnabled;
    private final boolean noclipEnabled;

    BurrowPhase(String id, boolean burrowing, boolean spectatorMotionEnabled, boolean noclipEnabled) {
        this.id = id;
        this.burrowing = burrowing;
        this.spectatorMotionEnabled = spectatorMotionEnabled;
        this.noclipEnabled = noclipEnabled;
    }

    public String id() {
        return id;
    }

    public boolean isBurrowing() {
        return burrowing;
    }

    public boolean spectatorMotionEnabled() {
        return spectatorMotionEnabled;
    }

    public boolean noclipEnabled() {
        return noclipEnabled;
    }

    public boolean isEntering() {
        return this == ENTERING_GHOST || this == ENTERING_DIG;
    }

    public boolean isActive() {
        return this == GHOST_ACTIVE || this == DIG_ACTIVE;
    }

    public static BurrowPhase fromId(String id) {
        for (BurrowPhase value : values()) {
            if (value.id.equalsIgnoreCase(id)) {
                return value;
            }
        }
        return NONE;
    }
}

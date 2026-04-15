package com.aiannotoke.pangolinsurvival.state;

public enum BurrowMode {
    NONE("none"),
    GHOST("ghost"),
    DIG("dig");

    private final String id;

    BurrowMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isBurrowing() {
        return this != NONE;
    }

    public static BurrowMode fromId(String id) {
        for (BurrowMode value : values()) {
            if (value.id.equalsIgnoreCase(id)) {
                return value;
            }
        }
        return NONE;
    }
}

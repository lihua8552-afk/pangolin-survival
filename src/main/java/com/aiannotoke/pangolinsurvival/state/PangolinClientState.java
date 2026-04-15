package com.aiannotoke.pangolinsurvival.state;

import com.aiannotoke.pangolinsurvival.network.PangolinStatePayload;

public class PangolinClientState {

    private boolean enabled;
    private BurrowMode mode = BurrowMode.NONE;
    private BurrowPhase phase = BurrowPhase.NONE;
    private float stamina;
    private int maxStamina;
    private int cooldownTicks;
    private HelmetBurrowTier helmetTier = HelmetBurrowTier.NONE;
    private float digProgressPercent;
    private boolean spectatorMotionEnabled;
    private boolean movementNoclipActive;
    private boolean burrowMediumRequired;

    public void accept(PangolinStatePayload payload) {
        enabled = payload.enabled();
        mode = BurrowMode.fromId(payload.mode());
        phase = BurrowPhase.fromId(payload.phase());
        stamina = payload.stamina();
        maxStamina = payload.maxStamina();
        cooldownTicks = payload.cooldownTicks();
        helmetTier = HelmetBurrowTier.fromId(payload.helmetTier());
        digProgressPercent = payload.digProgressPercent();
        spectatorMotionEnabled = payload.spectatorMotionEnabled();
        movementNoclipActive = payload.movementNoclipActive();
        burrowMediumRequired = payload.burrowMediumRequired();
    }

    public void reset() {
        enabled = false;
        mode = BurrowMode.NONE;
        phase = BurrowPhase.NONE;
        stamina = 0.0F;
        maxStamina = 0;
        cooldownTicks = 0;
        helmetTier = HelmetBurrowTier.NONE;
        digProgressPercent = 0.0F;
        spectatorMotionEnabled = false;
        movementNoclipActive = false;
        burrowMediumRequired = false;
    }

    public boolean enabled() {
        return enabled;
    }

    public BurrowMode mode() {
        return mode;
    }

    public BurrowPhase phase() {
        return phase;
    }

    public float stamina() {
        return stamina;
    }

    public int maxStamina() {
        return maxStamina;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public HelmetBurrowTier helmetTier() {
        return helmetTier;
    }

    public float digProgressPercent() {
        return digProgressPercent;
    }

    public boolean spectatorMotionEnabled() {
        return spectatorMotionEnabled;
    }

    public boolean movementNoclipActive() {
        return movementNoclipActive;
    }

    public boolean burrowMediumRequired() {
        return burrowMediumRequired;
    }
}

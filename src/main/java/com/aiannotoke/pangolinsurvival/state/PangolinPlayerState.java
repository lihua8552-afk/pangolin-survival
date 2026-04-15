package com.aiannotoke.pangolinsurvival.state;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PangolinPlayerState {

    private boolean enabled;
    private BurrowMode mode = BurrowMode.NONE;
    private BurrowPhase phase = BurrowPhase.NONE;
    private BurrowMode requestedMode = BurrowMode.NONE;
    private int maxStamina;
    private float stamina;
    private int cooldownTicks;
    private Vec3d lastSafePos;
    private Vec3d lastBurrowPos;
    private BlockPos digTargetPos;
    private int digTargetStateRawId = -1;
    private float digProgress;
    private long lastHintTick = Long.MIN_VALUE;
    private int airBoundaryTicks;
    private int phaseTicks;
    private boolean dirty = true;
    private HelmetBurrowTier helmetTier = HelmetBurrowTier.NONE;
    private boolean abilitySnapshotCaptured;
    private boolean previousAllowFlying;
    private boolean previousFlying;
    private float previousFlySpeed;

    public PangolinPlayerState(int maxStamina) {
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.dirty = true;
    }

    public BurrowMode mode() {
        return mode;
    }

    public void setMode(BurrowMode mode) {
        this.mode = mode;
        this.dirty = true;
    }

    public BurrowPhase phase() {
        return phase;
    }

    public void setPhase(BurrowPhase phase) {
        this.phase = phase;
        this.dirty = true;
    }

    public BurrowMode requestedMode() {
        return requestedMode;
    }

    public void setRequestedMode(BurrowMode requestedMode) {
        this.requestedMode = requestedMode;
        this.dirty = true;
    }

    public int maxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(int maxStamina) {
        this.maxStamina = maxStamina;
        this.dirty = true;
    }

    public float stamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = stamina;
        this.dirty = true;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
        this.dirty = true;
    }

    public Vec3d lastSafePos() {
        return lastSafePos;
    }

    public void setLastSafePos(Vec3d lastSafePos) {
        this.lastSafePos = lastSafePos;
    }

    public Vec3d lastBurrowPos() {
        return lastBurrowPos;
    }

    public void setLastBurrowPos(Vec3d lastBurrowPos) {
        this.lastBurrowPos = lastBurrowPos;
    }

    public BlockPos digTargetPos() {
        return digTargetPos;
    }

    public void setDigTargetPos(BlockPos digTargetPos) {
        this.digTargetPos = digTargetPos;
        this.dirty = true;
    }

    public int digTargetStateRawId() {
        return digTargetStateRawId;
    }

    public void setDigTargetStateRawId(int digTargetStateRawId) {
        this.digTargetStateRawId = digTargetStateRawId;
    }

    public float digProgress() {
        return digProgress;
    }

    public void setDigProgress(float digProgress) {
        this.digProgress = digProgress;
        this.dirty = true;
    }

    public long lastHintTick() {
        return lastHintTick;
    }

    public void setLastHintTick(long lastHintTick) {
        this.lastHintTick = lastHintTick;
    }

    public int airBoundaryTicks() {
        return airBoundaryTicks;
    }

    public void setAirBoundaryTicks(int airBoundaryTicks) {
        this.airBoundaryTicks = airBoundaryTicks;
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    public void setPhaseTicks(int phaseTicks) {
        this.phaseTicks = phaseTicks;
    }

    public boolean dirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public HelmetBurrowTier helmetTier() {
        return helmetTier;
    }

    public void setHelmetTier(HelmetBurrowTier helmetTier) {
        this.helmetTier = helmetTier;
        this.dirty = true;
    }

    public boolean abilitySnapshotCaptured() {
        return abilitySnapshotCaptured;
    }

    public void setAbilitySnapshotCaptured(boolean abilitySnapshotCaptured) {
        this.abilitySnapshotCaptured = abilitySnapshotCaptured;
    }

    public boolean previousAllowFlying() {
        return previousAllowFlying;
    }

    public void setPreviousAllowFlying(boolean previousAllowFlying) {
        this.previousAllowFlying = previousAllowFlying;
    }

    public boolean previousFlying() {
        return previousFlying;
    }

    public void setPreviousFlying(boolean previousFlying) {
        this.previousFlying = previousFlying;
    }

    public float previousFlySpeed() {
        return previousFlySpeed;
    }

    public void setPreviousFlySpeed(float previousFlySpeed) {
        this.previousFlySpeed = previousFlySpeed;
    }

    public void clearDigState() {
        this.digTargetPos = null;
        this.digTargetStateRawId = -1;
        this.digProgress = 0.0F;
        this.dirty = true;
    }
}

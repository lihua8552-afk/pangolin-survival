package com.aiannotoke.pangolinsurvival.service;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.config.ConfigManager;
import com.aiannotoke.pangolinsurvival.config.PangolinConfig;
import com.aiannotoke.pangolinsurvival.network.PangolinStatePayload;
import com.aiannotoke.pangolinsurvival.state.BurrowMode;
import com.aiannotoke.pangolinsurvival.state.BurrowPhase;
import com.aiannotoke.pangolinsurvival.state.HelmetBurrowTier;
import com.aiannotoke.pangolinsurvival.state.PangolinClientState;
import com.aiannotoke.pangolinsurvival.state.PangolinPlayerState;
import com.aiannotoke.pangolinsurvival.util.PangolinItems;
import com.aiannotoke.pangolinsurvival.util.PangolinTags;
import com.aiannotoke.pangolinsurvival.util.StaminaUpgradeRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PangolinService {

    private static final int HINT_COOLDOWN_TICKS = 10;
    private static final int ENTRY_MAX_TICKS = 24;
    private static final int EXIT_MAX_TICKS = 40;
    private static final int AIR_BOUNDARY_GRACE_TICKS = 4;
    private static final int DIG_AIR_BOUNDARY_GRACE_TICKS = 16;

    private final ConfigManager configManager;
    private final Map<UUID, PangolinPlayerState> playerStates = new HashMap<>();

    private PangolinConfig config;
    private MinecraftServer server;

    public PangolinService(ConfigManager configManager) {
        this.configManager = configManager;
        this.config = configManager.loadOrCreate();
    }

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        applyGlobalStateToOnlinePlayers();
    }

    public void onServerStopping(MinecraftServer server) {
        this.server = null;
        playerStates.clear();
    }

    public void onServerTick(MinecraftServer server) {
        this.server = server;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PangolinPlayerState state = getState(player);
            state.setEnabled(config.globalEnabled);
            state.setHelmetTier(getHelmetTier(player));
            int maxStamina = resolvePlayerMaxStamina(player.getUuid());
            if (state.maxStamina() != maxStamina) {
                state.setMaxStamina(maxStamina);
            }
            if (state.stamina() > state.maxStamina()) {
                state.setStamina(state.maxStamina());
            }

            if (!config.globalEnabled) {
                forceEndBurrow(player, state);
                syncPlayer(player, state);
                continue;
            }

            if (state.cooldownTicks() > 0) {
                state.setCooldownTicks(state.cooldownTicks() - 1);
            }

            if ((state.phase() == BurrowPhase.ENTERING_DIG || state.phase() == BurrowPhase.DIG_ACTIVE)
                    && !state.helmetTier().canDig()) {
                downgradeToGhost(player, state, "hint.pangolinsurvival.need_helmet");
            }

            switch (state.phase()) {
                case NONE -> tickSurface(player, state);
                case ENTERING_GHOST, ENTERING_DIG -> tickEntering(player, state);
                case GHOST_ACTIVE -> tickGhostActive(player, state);
                case DIG_ACTIVE -> tickDigActive(player, state);
                case EXITING -> tickExiting(player, state);
            }

            if (state.dirty() || state.phase().isBurrowing() || player.age % 10 == 0) {
                syncPlayer(player, state);
            }
        }
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        PangolinPlayerState state = getState(player);
        state.setMaxStamina(resolvePlayerMaxStamina(player.getUuid()));
        state.setStamina(state.maxStamina());
        initializeSurfaceState(player, state);
        if (config.globalEnabled && shouldGrantStarterHatOnJoin(player)) {
            ensureStarterHat(player);
        }
        syncPlayer(player, state);
    }

    public void onPlayerLeave(ServerPlayerEntity player) {
        PangolinPlayerState state = playerStates.remove(player.getUuid());
        if (state != null) {
            clearDigProgress(player, state);
            restorePlayerAbilities(player, state);
        }
    }

    public void onAfterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        PangolinPlayerState state = getState(newPlayer);
        state.setMaxStamina(resolvePlayerMaxStamina(newPlayer.getUuid()));
        state.setStamina(state.maxStamina());
        initializeSurfaceState(newPlayer, state);
        restorePlayerAbilities(newPlayer, state);
        if (config.globalEnabled) {
            ensureStarterHat(newPlayer);
        }
        syncPlayer(newPlayer, state);
    }

    public ActionResult onAttackBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (!isEnabledFor(player)) {
            return ActionResult.PASS;
        }

        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            if (isHoldingPickaxe(player)) {
                sendHint(serverPlayer, "hint.pangolinsurvival.pickaxe_disabled");
            } else if (getObservedPhase(player) != BurrowPhase.DIG_ACTIVE) {
                sendHint(serverPlayer, "hint.pangolinsurvival.need_dig");
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!isEnabledFor(player) || !getObservedPhase(player).isBurrowing()) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            sendHint(serverPlayer, "hint.pangolinsurvival.burrow_action_blocked");
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!isEnabledFor(player)) {
            return ActionResult.PASS;
        }

        BurrowPhase phase = getObservedPhase(player);
        if (!phase.isBurrowing()) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (phase == BurrowPhase.DIG_ACTIVE && stack.getItem() instanceof BlockItem) {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer && !consumePlacementStamina(serverPlayer)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        }

        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            sendHint(serverPlayer, "hint.pangolinsurvival.burrow_action_blocked");
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
        if (!isEnabledFor(player)) {
            return ActionResult.PASS;
        }

        BurrowPhase phase = getObservedPhase(player);
        if (!phase.isBurrowing()) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (phase == BurrowPhase.DIG_ACTIVE && stack.getItem() instanceof BlockItem) {
            return ActionResult.PASS;
        }

        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            sendHint(serverPlayer, "hint.pangolinsurvival.burrow_action_blocked");
        }
        return ActionResult.FAIL;
    }

    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (!isEnabledFor(player)) {
            return true;
        }
        return false;
    }

    public void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        // Manual block breaking is disabled while pangolin mode is active.
    }

    public void toggleGhost(ServerPlayerEntity player) {
        PangolinPlayerState state = getState(player);
        if (!config.globalEnabled) {
            sendHint(player, "hint.pangolinsurvival.global_disabled");
            return;
        }

        if (state.phase() == BurrowPhase.GHOST_ACTIVE || state.phase() == BurrowPhase.ENTERING_GHOST) {
            if (isBodyInsideBurrowMedium(player, true)) {
                if (!excavateSelfSpace(player, state)) {
                    return;
                }
                finishExit(player, state);
                player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.8F, 0.9F);
                sendHint(player, "hint.pangolinsurvival.surface_exit");
                return;
            }
            beginExit(player, state, true);
            return;
        }

        enterMode(player, state, BurrowMode.GHOST);
    }

    public void toggleDig(ServerPlayerEntity player) {
        PangolinPlayerState state = getState(player);
        if (!config.globalEnabled) {
            sendHint(player, "hint.pangolinsurvival.global_disabled");
            return;
        }

        if (state.phase() == BurrowPhase.DIG_ACTIVE || state.phase() == BurrowPhase.ENTERING_DIG) {
            beginExit(player, state, true);
            return;
        }

        enterMode(player, state, BurrowMode.DIG);
    }

    public void setGlobalEnabled(boolean enabled) {
        config.globalEnabled = enabled;
        configManager.save(config);
        applyGlobalStateToOnlinePlayers();
    }

    public void reloadConfig() {
        boolean oldGlobalEnabled = config.globalEnabled;
        config = configManager.reload();
        if (oldGlobalEnabled != config.globalEnabled) {
            applyGlobalStateToOnlinePlayers();
        } else if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                syncPlayer(player, getState(player));
            }
        }
    }

    public PangolinConfig getConfig() {
        return config;
    }

    public PangolinPlayerState getState(ServerPlayerEntity player) {
        return playerStates.computeIfAbsent(player.getUuid(), ignored -> {
            PangolinPlayerState state = new PangolinPlayerState(resolvePlayerMaxStamina(player.getUuid()));
            state.setEnabled(config.globalEnabled);
            return state;
        });
    }

    public BurrowMode getObservedMode(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) {
            return getClientState().mode();
        }
        PangolinPlayerState state = playerStates.get(player.getUuid());
        return state != null ? state.mode() : getState((ServerPlayerEntity) player).mode();
    }

    public BurrowPhase getObservedPhase(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) {
            return getClientState().phase();
        }
        PangolinPlayerState state = playerStates.get(player.getUuid());
        return state != null ? state.phase() : getState((ServerPlayerEntity) player).phase();
    }

    public HelmetBurrowTier getObservedHelmetTier(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) {
            return getClientState().helmetTier();
        }
        return getHelmetTier(player);
    }

    public int getObservedMaxStamina(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) {
            int clientMaxStamina = getClientState().maxStamina();
            return clientMaxStamina > 0
                    ? clientMaxStamina
                    : StaminaUpgradeRules.normalizeBaseMax(config.maxStamina);
        }
        PangolinPlayerState state = playerStates.get(player.getUuid());
        return state != null ? state.maxStamina() : getState((ServerPlayerEntity) player).maxStamina();
    }

    public int getUpgradeCostLevels(PlayerEntity player) {
        return StaminaUpgradeRules.getUpgradeCostLevels(config.maxStamina, getObservedMaxStamina(player));
    }

    public int getNextUpgradeMaxStamina(PlayerEntity player) {
        return StaminaUpgradeRules.getNextMaxStamina(config.maxStamina, getObservedMaxStamina(player));
    }

    public void upgradeMaxStamina(ServerPlayerEntity player) {
        PangolinPlayerState state = getState(player);
        int currentMax = state.maxStamina();
        int nextMax = StaminaUpgradeRules.getNextMaxStamina(config.maxStamina, currentMax);
        if (nextMax <= currentMax) {
            sendHint(player, "hint.pangolinsurvival.stamina_maxed");
            return;
        }

        int levelCost = StaminaUpgradeRules.getUpgradeCostLevels(config.maxStamina, currentMax);
        if (player.experienceLevel < levelCost) {
            sendHint(player, "hint.pangolinsurvival.need_levels");
            return;
        }

        player.addExperienceLevels(-levelCost);
        savePlayerMaxStamina(player.getUuid(), nextMax);
        state.setMaxStamina(nextMax);
        state.setStamina(nextMax);
        syncPlayer(player, state);
        sendHint(player, "hint.pangolinsurvival.stamina_upgraded");
    }

    public boolean shouldOverrideMining(PlayerEntity player) {
        return isEnabledFor(player);
    }

    public boolean shouldDisableNaturalHealing(PlayerEntity player) {
        return isEnabledFor(player) && getObservedPhase(player).isBurrowing();
    }

    public boolean shouldPreventAirRecovery(PlayerEntity player) {
        return isEnabledFor(player)
                && getObservedPhase(player).isBurrowing()
                && isBodyInsideBurrowMedium(player, false);
    }

    public boolean isBurrowing(PlayerEntity player) {
        return getObservedPhase(player).isBurrowing();
    }

    public boolean isMovementNoclipActive(PlayerEntity player) {
        BurrowPhase phase = getObservedPhase(player);
        return phase == BurrowPhase.ENTERING_GHOST
                || phase == BurrowPhase.GHOST_ACTIVE
                || phase == BurrowPhase.ENTERING_DIG
                || phase == BurrowPhase.DIG_ACTIVE
                || phase == BurrowPhase.EXITING;
    }

    public boolean isBurrowMediumRequired(PlayerEntity player) {
        BurrowPhase phase = getObservedPhase(player);
        return phase == BurrowPhase.ENTERING_GHOST
                || phase == BurrowPhase.GHOST_ACTIVE
                || phase == BurrowPhase.ENTERING_DIG
                || phase == BurrowPhase.DIG_ACTIVE;
    }

    public boolean canApplyBurrowMovement(PlayerEntity player, Vec3d movement) {
        if (!isMovementNoclipActive(player)) {
            return true;
        }

        World world = player.getEntityWorld();
        BurrowPhase phase = getObservedPhase(player);
        if (phase == BurrowPhase.EXITING) {
            return movement.y >= -1.0E-4D;
        }
        Vec3d probeDirection = movement.lengthSquared() > 1.0E-4D ? movement : player.getRotationVector();
        for (BlockPos probe : getBoundaryProbesAtPosition(
                player.getX() + movement.x,
                player.getY() + movement.y,
                player.getZ() + movement.z,
                probeDirection,
                true
        )) {
            BlockState state = world.getBlockState(probe);
            if (!state.isAir() && isBurrowBlockedState(state, world, probe)) {
                return false;
            }
        }

        if (phase == BurrowPhase.DIG_ACTIVE) {
            PangolinPlayerState state = getMovementState(player);
            if (state == null) {
                return true;
            }
            if (world.isClient()) {
                return canClientTraverseDigPath(player, state, movement);
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                return prepareDigPathForMovement(serverPlayer, state, movement);
            }
        }
        return true;
    }

    public float getCustomBreakingSpeed(PlayerEntity player, BlockState state) {
        if (!isEnabledFor(player)) {
            return player.getMainHandStack().getMiningSpeedMultiplier(state);
        }
        if (isHoldingPickaxe(player)) {
            return 0.0F;
        }
        if (getObservedPhase(player) != BurrowPhase.DIG_ACTIVE) {
            return 0.0F;
        }

        HelmetBurrowTier tier = getObservedHelmetTier(player);
        if (!tier.canDig() || !tier.canHarvest(state)) {
            return 0.0F;
        }
        return tier.effectiveMiningSpeed(state);
    }

    public boolean canObservedHarvest(PlayerEntity player, BlockState state) {
        if (!isEnabledFor(player)) {
            return player.getMainHandStack().isSuitableFor(state);
        }
        if (isHoldingPickaxe(player) || getObservedPhase(player) != BurrowPhase.DIG_ACTIVE) {
            return false;
        }
        return getObservedHelmetTier(player).canHarvest(state);
    }

    private void tickSurface(ServerPlayerEntity player, PangolinPlayerState state) {
        clearDigProgress(player, state);
        restorePlayerAbilities(player, state);
        recoverStamina(state);
        rememberSafePos(player, state);
    }

    private void tickEntering(ServerPlayerEntity player, PangolinPlayerState state) {
        ensureGhostEnteringAbilities(player, state);
        state.setPhaseTicks(state.phaseTicks() + 1);
        clearDigProgress(player, state);

        Vec3d velocity = player.getVelocity();
        double nextY = velocity.y;
        if (nextY > -0.08D) {
            nextY = -0.08D;
        }
        player.setVelocity(0.0D, nextY, 0.0D);

        if (isBodyInsideBurrowMedium(player, true)) {
            state.setPhase(state.requestedMode() == BurrowMode.DIG ? BurrowPhase.DIG_ACTIVE : BurrowPhase.GHOST_ACTIVE);
            state.setPhaseTicks(0);
            state.setAirBoundaryTicks(0);
            state.setLastBurrowPos(currentPos(player));
            ensureActiveBurrowAbilities(player, state);
            return;
        }

        int maxTicks = (state.phase() == BurrowPhase.ENTERING_GHOST || state.phase() == BurrowPhase.ENTERING_DIG) ? 60 : ENTRY_MAX_TICKS;
        if (state.phaseTicks() > maxTicks) {
            beginExit(player, state, false);
        }
    }

    private void tickGhostActive(ServerPlayerEntity player, PangolinPlayerState state) {
        ensureActiveBurrowAbilities(player, state);
        clearDigProgress(player, state);
        state.setPhaseTicks(state.phaseTicks() + 1);
        state.setLastBurrowPos(currentPos(player));

        if (containsForbiddenBodyProbe(player)) {
            beginExit(player, state, false);
            return;
        }

        if (!isBodyInsideBurrowMedium(player, false)) {
            beginExit(player, state, false);
            return;
        }

        updateAirBoundary(player, state);
    }

    private void tickDigActive(ServerPlayerEntity player, PangolinPlayerState state) {
        ensureActiveBurrowAbilities(player, state);
        state.setPhaseTicks(state.phaseTicks() + 1);
        state.setLastBurrowPos(currentPos(player));

        if (containsForbiddenBodyProbe(player)) {
            beginExit(player, state, false);
            return;
        }

        updateAirBoundary(player, state);
        tickDigProgress(player, state);
    }

    private void tickExiting(ServerPlayerEntity player, PangolinPlayerState state) {
        ensureTransitionBurrowAbilities(player, state);
        state.setPhaseTicks(state.phaseTicks() + 1);
        clearDigProgress(player, state);

        if (isBodyInsideBurrowMedium(player, false) && !excavateSelfSpace(player, state)) {
            player.setVelocity(0.0D, Math.max(player.getVelocity().y, 0.08D), 0.0D);
            if (state.phaseTicks() > EXIT_MAX_TICKS) {
                relocateToLastSafePos(player, state);
                finishExit(player, state);
            }
            return;
        }

        player.setVelocity(0.0D, Math.max(player.getVelocity().y + 0.06D, 0.18D), 0.0D);

        if (hasOpenAirBody(player) && !isBodyInsideBurrowMedium(player, false)) {
            finishExit(player, state);
            return;
        }

        if (state.phaseTicks() > EXIT_MAX_TICKS) {
            relocateToLastSafePos(player, state);
            finishExit(player, state);
        }
    }

    private void enterMode(ServerPlayerEntity player, PangolinPlayerState state, BurrowMode targetMode) {
        if (player.hasVehicle() || player.isTouchingWater() || player.isSubmergedInWater() || player.isInLava()) {
            sendHint(player, "hint.pangolinsurvival.burrow_blocked");
            return;
        }

        if (targetMode == BurrowMode.DIG && !validateDigRequirements(player, state)) {
            return;
        }

        if (state.phase() == BurrowPhase.GHOST_ACTIVE || state.phase() == BurrowPhase.DIG_ACTIVE) {
            if (targetMode == BurrowMode.GHOST) {
                clearDigProgress(player, state);
                state.setMode(BurrowMode.GHOST);
                state.setRequestedMode(BurrowMode.GHOST);
                state.setPhase(BurrowPhase.GHOST_ACTIVE);
                state.setPhaseTicks(0);
                ensureActiveBurrowAbilities(player, state);
                sendHint(player, "hint.pangolinsurvival.ghost_enter");
                return;
            }

            if (state.phase() != BurrowPhase.DIG_ACTIVE) {
                state.setStamina(Math.max(0.0F, state.stamina() - config.digEnterCost));
            }
            state.setMode(BurrowMode.DIG);
            state.setRequestedMode(BurrowMode.DIG);
            state.setPhase(BurrowPhase.DIG_ACTIVE);
            state.setPhaseTicks(0);
            state.setAirBoundaryTicks(0);
            clearDigProgress(player, state);
            ensureActiveBurrowAbilities(player, state);
            sendHint(player, "hint.pangolinsurvival.dig_enter");
            return;
        }

        if (!canInitiateBurrow(player)) {
            sendHint(player, "hint.pangolinsurvival.burrow_blocked");
            return;
        }

        state.setMode(targetMode);
        state.setRequestedMode(targetMode);
        state.setPhase(targetMode == BurrowMode.DIG ? BurrowPhase.ENTERING_DIG : BurrowPhase.ENTERING_GHOST);
        state.setPhaseTicks(0);
        state.setAirBoundaryTicks(0);
        state.setLastBurrowPos(currentPos(player));
        if (state.lastSafePos() == null) {
            state.setLastSafePos(currentPos(player));
        }
        clearDigProgress(player, state);
        if (targetMode == BurrowMode.GHOST || targetMode == BurrowMode.DIG) {
            ensureGhostEnteringAbilities(player, state);
        } else {
            ensureTransitionBurrowAbilities(player, state);
        }
        player.playSound(targetMode == BurrowMode.GHOST ? SoundEvents.BLOCK_GRAVEL_BREAK : SoundEvents.BLOCK_DEEPSLATE_BREAK, 0.8F, 1.0F);
        sendHint(player, targetMode == BurrowMode.GHOST ? "hint.pangolinsurvival.ghost_enter" : "hint.pangolinsurvival.dig_enter");
    }

    private boolean validateDigRequirements(ServerPlayerEntity player, PangolinPlayerState state) {
        HelmetBurrowTier tier = getHelmetTier(player);
        if (!tier.canDig()) {
            sendHint(player, "hint.pangolinsurvival.need_helmet");
            return false;
        }
        if (state.stamina() < config.digEnterCost) {
            sendHint(player, "hint.pangolinsurvival.low_stamina");
            return false;
        }
        return true;
    }

    private void beginExit(ServerPlayerEntity player, PangolinPlayerState state, boolean announce) {
        if (state.phase() == BurrowPhase.NONE || state.phase() == BurrowPhase.EXITING) {
            return;
        }

        clearDigProgress(player, state);
        state.setPhase(BurrowPhase.EXITING);
        state.setPhaseTicks(0);
        state.setAirBoundaryTicks(0);
        ensureTransitionBurrowAbilities(player, state);

        if (announce) {
            player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.8F, 0.9F);
            sendHint(player, "hint.pangolinsurvival.surface_exit");
        }
    }

    private void finishExit(ServerPlayerEntity player, PangolinPlayerState state) {
        clearDigProgress(player, state);
        restorePlayerAbilities(player, state);
        initializeSurfaceState(player, state);
    }

    private void forceEndBurrow(ServerPlayerEntity player, PangolinPlayerState state) {
        clearDigProgress(player, state);
        restorePlayerAbilities(player, state);
        initializeSurfaceState(player, state);
    }

    private void downgradeToGhost(ServerPlayerEntity player, PangolinPlayerState state, String hintKey) {
        clearDigProgress(player, state);
        state.setMode(BurrowMode.GHOST);
        state.setRequestedMode(BurrowMode.GHOST);
        state.setPhase(BurrowPhase.GHOST_ACTIVE);
        state.setPhaseTicks(0);
        state.setAirBoundaryTicks(0);
        ensureActiveBurrowAbilities(player, state);
        player.playSound(SoundEvents.BLOCK_GRAVEL_BREAK, 0.8F, 0.7F);
        sendHint(player, hintKey);
    }

    private void ensureActiveBurrowAbilities(ServerPlayerEntity player, PangolinPlayerState state) {
        captureAbilitiesIfNeeded(player, state);
        player.noClip = true;
        player.setSprinting(false);

        boolean flightEnabled = shouldEnableBurrowFlight(player, state);
        float flySpeed = state.phase() == BurrowPhase.DIG_ACTIVE ? config.digFlySpeed : config.ghostFlySpeed;
        applyBurrowFlightAbilities(player, state, flightEnabled, flySpeed);
    }

    private void ensureTransitionBurrowAbilities(ServerPlayerEntity player, PangolinPlayerState state) {
        captureAbilitiesIfNeeded(player, state);
        player.noClip = true;
        player.setNoGravity(state.phase() == BurrowPhase.EXITING);
        player.setSprinting(false);

        if (!player.getAbilities().creativeMode) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
    }

    private void ensureGhostEnteringAbilities(ServerPlayerEntity player, PangolinPlayerState state) {
        captureAbilitiesIfNeeded(player, state);
        player.noClip = true;
        player.setNoGravity(false);
        player.setSprinting(false);

        if (!player.getAbilities().creativeMode) {
            boolean changed = player.getAbilities().allowFlying
                    || player.getAbilities().flying
                    || Math.abs(player.getAbilities().getFlySpeed() - state.previousFlySpeed()) > 1.0E-4F;
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.getAbilities().setFlySpeed(state.previousFlySpeed());
            if (changed) {
                player.sendAbilitiesUpdate();
            }
        }
    }

    private void captureAbilitiesIfNeeded(ServerPlayerEntity player, PangolinPlayerState state) {
        if (state.abilitySnapshotCaptured()) {
            return;
        }
        state.setPreviousAllowFlying(player.getAbilities().allowFlying);
        state.setPreviousFlying(player.getAbilities().flying);
        state.setPreviousFlySpeed(player.getAbilities().getFlySpeed());
        state.setAbilitySnapshotCaptured(true);
    }

    private void restorePlayerAbilities(ServerPlayerEntity player, PangolinPlayerState state) {
        player.noClip = false;
        player.setNoGravity(false);

        if (state.abilitySnapshotCaptured() && !player.getAbilities().creativeMode) {
            player.getAbilities().allowFlying = state.previousAllowFlying();
            player.getAbilities().flying = state.previousFlying();
            player.getAbilities().setFlySpeed(state.previousFlySpeed());
            player.sendAbilitiesUpdate();
        }
        state.setAbilitySnapshotCaptured(false);
    }

    private void applyBurrowFlightAbilities(ServerPlayerEntity player, PangolinPlayerState state, boolean flightEnabled, float flySpeed) {
        player.setNoGravity(flightEnabled);

        if (player.getAbilities().creativeMode) {
            return;
        }

        boolean changed = player.getAbilities().allowFlying != flightEnabled
                || player.getAbilities().flying != flightEnabled;
        float targetFlySpeed = flightEnabled ? flySpeed : state.previousFlySpeed();
        if (Math.abs(player.getAbilities().getFlySpeed() - targetFlySpeed) > 1.0E-4F) {
            player.getAbilities().setFlySpeed(targetFlySpeed);
            changed = true;
        }
        player.getAbilities().allowFlying = flightEnabled;
        player.getAbilities().flying = flightEnabled;
        if (changed) {
            player.sendAbilitiesUpdate();
        }
    }

    private boolean shouldEnableBurrowFlight(ServerPlayerEntity player, PangolinPlayerState state) {
        return switch (state.phase()) {
            case ENTERING_GHOST, ENTERING_DIG -> false;
            case DIG_ACTIVE -> isBurrowEnvironmentSatisfied(player, state);
            case GHOST_ACTIVE -> true;
            default -> false;
        };
    }

    private void updateAirBoundary(ServerPlayerEntity player, PangolinPlayerState state) {
        if (isBurrowEnvironmentSatisfied(player, state)) {
            state.setAirBoundaryTicks(0);
            return;
        }

        state.setAirBoundaryTicks(state.airBoundaryTicks() + 1);
        int maxTicks = state.phase() == BurrowPhase.DIG_ACTIVE ? DIG_AIR_BOUNDARY_GRACE_TICKS : AIR_BOUNDARY_GRACE_TICKS;
        if (state.airBoundaryTicks() > maxTicks) {
            beginExit(player, state, false);
        }
    }

    private boolean isBurrowEnvironmentSatisfied(ServerPlayerEntity player, PangolinPlayerState state) {
        if (isBodyInsideBurrowMedium(player, true)) {
            return true;
        }
        return state.phase() == BurrowPhase.DIG_ACTIVE && hasDigTunnelSupport(player, state);
    }

    private boolean hasDigTunnelSupport(ServerPlayerEntity player, PangolinPlayerState state) {
        World world = player.getEntityWorld();
        for (BlockPos probe : getDigSupportProbes(player, state)) {
            if (isBurrowMedium(world.getBlockState(probe), world, probe)) {
                return true;
            }
        }

        BlockPos targetPos = determineDigTarget(player, state);
        return targetPos != null && hasBurrowMediumColumn(world, targetPos);
    }

    private List<BlockPos> getDigSupportProbes(ServerPlayerEntity player, PangolinPlayerState state) {
        BlockPos feet = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
        BlockPos body = feet.up();
        BlockPos ceiling = feet.up(2);
        Direction forward = getHorizontalDigFacing(player, state);
        Direction left = forward.rotateYCounterclockwise();
        Direction right = forward.rotateYClockwise();

        List<BlockPos> probes = new ArrayList<>(8);
        probes.add(ceiling);
        probes.add(ceiling.offset(forward));
        probes.add(feet.offset(left));
        probes.add(body.offset(left));
        probes.add(feet.offset(right));
        probes.add(body.offset(right));
        probes.add(feet.offset(forward));
        probes.add(body.offset(forward));
        return probes;
    }

    public void tickPlayerBurrowAir(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !isEnabledFor(serverPlayer)) {
            return;
        }

        BurrowPhase phase = getObservedPhase(serverPlayer);
        if (!phase.isBurrowing()) {
            return;
        }

        if (!isBodyInsideBurrowMedium(serverPlayer, false)) {
            if (serverPlayer.getAir() < serverPlayer.getMaxAir()) {
                serverPlayer.setAir(serverPlayer.getMaxAir());
            }
            return;
        }

        int nextAir = serverPlayer.getAir() - 1;
        if (nextAir <= -20) {
            nextAir = 0;
            serverPlayer.damage((ServerWorld) serverPlayer.getEntityWorld(), serverPlayer.getDamageSources().drown(), 2.0F);
        }
        serverPlayer.setAir(nextAir);
    }

    private void tickDigProgress(ServerPlayerEntity player, PangolinPlayerState state) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        HelmetBurrowTier tier = getHelmetTier(player);
        if (!tier.canDig()) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.need_helmet");
            return;
        }

        BlockPos targetPos = determineDigTarget(player, state);
        if (targetPos == null) {
            clearDigProgress(player, state);
            return;
        }

        if (!canAutoDigColumn(world, targetPos, tier)) {
            clearDigProgress(player, state);
            return;
        }

        int targetSignature = getDigColumnSignature(world, targetPos);
        if (!targetPos.equals(state.digTargetPos()) || state.digTargetStateRawId() != targetSignature) {
            clearDigProgress(player, state);
            state.setDigTargetPos(targetPos);
            state.setDigTargetStateRawId(targetSignature);
            state.setDigProgress(0.0F);
        }

        state.setDigProgress(Math.min(1.0F, state.digProgress() + getDigProgressPerTick(tier, world, targetPos)));
        int stage = Math.max(0, Math.min(9, (int) (state.digProgress() * 10.0F) - 1));
        applyDigStage(world, player, targetPos, stage);

        if (state.digProgress() < 1.0F) {
            return;
        }

        float staminaCost = getDigColumnCost(world, targetPos, tier);
        if (state.stamina() < staminaCost) {
            clearDigProgress(player, state);
            downgradeToGhost(player, state, "hint.pangolinsurvival.low_stamina");
            return;
        }

        boolean broke = breakDigColumn(world, player, targetPos);
        clearDigProgress(player, state);
        if (!broke) {
            return;
        }

        state.setStamina(Math.max(0.0F, state.stamina() - staminaCost));
        if (state.stamina() <= 0.0F) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.low_stamina");
        }
    }

    private BlockPos determineDigTarget(PlayerEntity player, PangolinPlayerState state) {
        Vec3d direction = getDigDirection(player, state);
        if (direction.lengthSquared() < 1.0E-4D) {
            if (state.digTargetPos() != null) {
                return state.digTargetPos();
            }
            direction = player.getRotationVector();
            if (direction.lengthSquared() < 1.0E-4D) {
                return null;
            }
        }
        return determineDigTargetAtPosition(player.getX(), player.getY(), player.getZ(), direction);
    }

    private Vec3d getDigDirection(PlayerEntity player, PangolinPlayerState state) {
        Vec3d velocity = player.getVelocity();
        Vec3d preferredVelocity = preferHorizontalDigDirection(velocity);
        if (preferredVelocity != null && preferredVelocity.lengthSquared() > 0.0025D) {
            return preferredVelocity;
        }
        if (state.digTargetPos() != null) {
            Vec3d targetCenter = Vec3d.ofCenter(state.digTargetPos());
            Vec3d towardTarget = targetCenter.subtract(new Vec3d(player.getX(), player.getY() + 0.9D, player.getZ()));
            Vec3d preferredTargetDirection = preferHorizontalDigDirection(towardTarget);
            if (preferredTargetDirection != null) {
                return preferredTargetDirection;
            }
        }
        Vec3d lookDirection = preferHorizontalDigDirection(player.getRotationVector());
        return lookDirection == null ? player.getRotationVector() : lookDirection;
    }

    private Vec3d preferHorizontalDigDirection(Vec3d vector) {
        if (vector.lengthSquared() < 1.0E-4D) {
            return null;
        }

        double horizontalLengthSquared = vector.x * vector.x + vector.z * vector.z;
        if (horizontalLengthSquared > 1.0E-4D && Math.abs(vector.y) < 0.55D) {
            return new Vec3d(vector.x, 0.0D, vector.z).normalize();
        }
        return vector.normalize();
    }

    private Direction getHorizontalDigFacing(PlayerEntity player, PangolinPlayerState state) {
        Vec3d direction = getDigDirection(player, state);
        if (Math.abs(direction.x) > Math.abs(direction.z)) {
            return direction.x >= 0.0D ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(direction.z) > 1.0E-4D) {
            return direction.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
        return player.getHorizontalFacing();
    }

    private float getDigProgressPerTick(HelmetBurrowTier tier, World world, BlockPos pos) {
        float hardness = getDigColumnHardness(world, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        if (hardness == 0.0F) {
            return 1.0F;
        }
        float speed = Math.max(1.0F, getDigColumnSpeed(tier, world, pos));
        return speed / hardness / 20.0F;
    }

    private boolean excavateSelfSpace(ServerPlayerEntity player, PangolinPlayerState state) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        List<BlockPos> space = getExitExcavationSpace(player);
        float totalCost = 0.0F;
        HelmetBurrowTier tier = getHelmetTier(player);

        for (BlockPos pos : space) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir()) {
                continue;
            }
            if (isBurrowBlockedState(blockState, world, pos) || !tier.canHarvest(blockState)) {
                sendHint(player, "hint.pangolinsurvival.burrow_blocked");
                return false;
            }
            totalCost += getBreakCost(blockState, world, pos, tier);
        }

        if (totalCost <= 0.0F) {
            return true;
        }

        if (state.stamina() < totalCost) {
            sendHint(player, "hint.pangolinsurvival.low_stamina");
            return false;
        }

        for (BlockPos pos : space) {
            BlockState blockState = world.getBlockState(pos);
            if (!blockState.isAir()) {
                world.breakBlock(pos, true, player, 512);
            }
        }
        state.setStamina(Math.max(0.0F, state.stamina() - totalCost));
        return true;
    }

    private List<BlockPos> getExitExcavationSpace(ServerPlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>(3);
        BlockPos feet = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
        positions.add(feet);
        positions.add(feet.up());
        positions.add(feet.up(2));
        return positions;
    }

    private boolean canAutoDigColumn(World world, BlockPos pos, HelmetBurrowTier tier) {
        boolean anySolid = false;
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (state.isAir()) {
                continue;
            }
            anySolid = true;
            if (isBurrowBlockedState(state, world, target) || !tier.canHarvest(state)) {
                return false;
            }
        }
        return anySolid;
    }

    private boolean isDigColumnClear(World world, BlockPos pos) {
        for (BlockPos target : getDigColumnPositions(pos)) {
            if (!world.getBlockState(target).isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBurrowMediumColumn(World world, BlockPos pos) {
        for (BlockPos target : getDigColumnPositions(pos)) {
            if (isBurrowMedium(world.getBlockState(target), world, target)) {
                return true;
            }
        }
        return false;
    }

    private PangolinPlayerState getMovementState(PlayerEntity player) {
        PangolinPlayerState state = playerStates.get(player.getUuid());
        if (state == null && !player.getEntityWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            state = getState(serverPlayer);
        }
        return state;
    }

    private boolean canClientTraverseDigPath(PlayerEntity player, PangolinPlayerState state, Vec3d movement) {
        World world = player.getEntityWorld();
        HelmetBurrowTier tier = getObservedHelmetTier(player);
        if (!tier.canDig()) {
            return false;
        }

        for (BlockPos blockPos : collectMovementDigBlocks(player, state, movement)) {
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                continue;
            }
            if (isBurrowBlockedState(blockState, world, blockPos) || !tier.canHarvest(blockState)) {
                return false;
            }
        }
        return true;
    }

    private boolean prepareDigPathForMovement(ServerPlayerEntity player, PangolinPlayerState state, Vec3d movement) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        HelmetBurrowTier tier = getHelmetTier(player);
        if (!tier.canDig()) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.need_helmet");
            return false;
        }

        Set<BlockPos> blockTargets = collectMovementDigBlocks(player, state, movement);
        float totalCost = 0.0F;
        boolean anySolid = false;

        for (BlockPos blockPos : blockTargets) {
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                continue;
            }
            anySolid = true;
            if (isBurrowBlockedState(blockState, world, blockPos) || !tier.canHarvest(blockState)) {
                return false;
            }
            totalCost += getBreakCost(blockState, world, blockPos, tier);
        }

        if (!anySolid) {
            return true;
        }
        if (state.stamina() < totalCost) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.low_stamina");
            return false;
        }

        for (BlockPos blockPos : blockTargets) {
            BlockState blockState = world.getBlockState(blockPos);
            if (!blockState.isAir()) {
                world.breakBlock(blockPos, true, player, 512);
            }
        }

        state.setStamina(Math.max(0.0F, state.stamina() - totalCost));
        if (state.stamina() <= 0.0F) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.low_stamina");
        }
        clearDigProgress(player, state);
        return true;
    }

    private Set<BlockPos> collectMovementDigBlocks(PlayerEntity player, PangolinPlayerState state, Vec3d movement) {
        Set<BlockPos> blocks = new LinkedHashSet<>();
        for (BlockPos base : collectMovementDigColumnBases(player, state, movement)) {
            blocks.addAll(getDigColumnPositions(base));
        }
        return blocks;
    }

    private Set<BlockPos> collectMovementDigColumnBases(PlayerEntity player, PangolinPlayerState state, Vec3d movement) {
        Set<BlockPos> bases = new LinkedHashSet<>();
        double destX = player.getX() + movement.x;
        double destY = player.getY() + movement.y;
        double destZ = player.getZ() + movement.z;

        bases.add(BlockPos.ofFloored(destX, destY + 0.05D, destZ));

        Vec3d direction = movement.lengthSquared() > 1.0E-4D ? movement.normalize() : getDigDirection(player, state);
        BlockPos aheadTarget = determineDigTargetAtPosition(destX, destY, destZ, direction);
        if (aheadTarget != null) {
            bases.add(aheadTarget);
        }
        return bases;
    }

    private BlockPos determineDigTargetAtPosition(double x, double y, double z, Vec3d direction) {
        if (direction.lengthSquared() < 1.0E-4D) {
            return null;
        }

        Vec3d normalized = direction.normalize();
        Vec3d origin = new Vec3d(x, y + 0.15D, z);
        Vec3d probe = origin.add(normalized.multiply(0.65D));
        return BlockPos.ofFloored(probe);
    }

    private List<BlockPos> getDigColumnPositions(BlockPos base) {
        List<BlockPos> positions = new ArrayList<>(2);
        positions.add(base);
        positions.add(base.up());
        return positions;
    }

    private int getDigColumnSignature(World world, BlockPos pos) {
        int signature = 1;
        for (BlockPos target : getDigColumnPositions(pos)) {
            signature = 31 * signature + Block.getRawIdFromState(world.getBlockState(target));
        }
        return signature;
    }

    private float getDigColumnHardness(World world, BlockPos pos) {
        float hardest = 0.0F;
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (state.isAir()) {
                continue;
            }
            hardest = Math.max(hardest, state.getHardness(world, target));
        }
        return hardest;
    }

    private float getDigColumnSpeed(HelmetBurrowTier tier, World world, BlockPos pos) {
        float slowest = Float.MAX_VALUE;
        boolean anySolid = false;
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (state.isAir()) {
                continue;
            }
            anySolid = true;
            slowest = Math.min(slowest, Math.max(1.0F, tier.effectiveMiningSpeed(state)));
        }
        return anySolid ? slowest : 1.0F;
    }

    private float getDigColumnCost(World world, BlockPos pos, HelmetBurrowTier tier) {
        float total = 0.0F;
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (!state.isAir()) {
                total += getBreakCost(state, world, target, tier);
            }
        }
        return total;
    }

    private boolean breakDigColumn(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        boolean brokeAny = false;
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (!state.isAir()) {
                brokeAny |= world.breakBlock(target, true, player, 512);
            }
        }
        return brokeAny;
    }

    private void applyDigStage(ServerWorld world, ServerPlayerEntity player, BlockPos pos, int stage) {
        for (BlockPos target : getDigColumnPositions(pos)) {
            BlockState state = world.getBlockState(target);
            if (!state.isAir() || stage < 0) {
                world.setBlockBreakingInfo(player.getId(), target, stage);
            }
        }
    }

    private void clearDigProgress(ServerPlayerEntity player, PangolinPlayerState state) {
        if (state.digTargetPos() != null && player.getEntityWorld() instanceof ServerWorld serverWorld) {
            applyDigStage(serverWorld, player, state.digTargetPos(), -1);
        }
        state.clearDigState();
    }

    private boolean isBodyInsideBurrowMedium(PlayerEntity player, boolean includeForwardProbe) {
        World world = player.getEntityWorld();
        for (BlockPos probe : getBoundaryProbes(player, includeForwardProbe)) {
            if (isBurrowMedium(world.getBlockState(probe), world, probe)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsForbiddenBodyProbe(PlayerEntity player) {
        World world = player.getEntityWorld();
        for (BlockPos probe : getBodyProbes(player)) {
            BlockState state = world.getBlockState(probe);
            if (state.isAir()) {
                continue;
            }
            if (isBurrowBlockedState(state, world, probe)) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> getBoundaryProbes(PlayerEntity player, boolean includeForwardProbe) {
        return getBoundaryProbesAtPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getRotationVector(),
                includeForwardProbe
        );
    }

    private List<BlockPos> getBoundaryProbesAtPosition(double x, double y, double z, Vec3d forwardVector, boolean includeForwardProbe) {
        List<BlockPos> probes = new ArrayList<>(getBodyProbesAtPosition(x, y, z));
        if (includeForwardProbe) {
            Vec3d forward = forwardVector.normalize();
            Vec3d probePoint = new Vec3d(x, y + 0.9D, z).add(forward.multiply(0.65D));
            probes.add(BlockPos.ofFloored(probePoint.x, probePoint.y, probePoint.z));
        }
        return probes;
    }

    private List<BlockPos> getBodyProbes(PlayerEntity player) {
        return getBodyProbesAtPosition(player.getX(), player.getY(), player.getZ());
    }

    private List<BlockPos> getBodyProbesAtPosition(double x, double y, double z) {
        List<BlockPos> probes = new ArrayList<>(3);
        probes.add(BlockPos.ofFloored(x, y, z));
        probes.add(BlockPos.ofFloored(x, y + 0.9D, z));
        probes.add(BlockPos.ofFloored(x, y + 1.62D, z));
        return probes;
    }

    private boolean hasOpenAirBody(PlayerEntity player) {
        World world = player.getEntityWorld();
        for (BlockPos probe : getBodyProbes(player)) {
            if (!world.getBlockState(probe).isAir() || !world.getFluidState(probe).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void initializeSurfaceState(ServerPlayerEntity player, PangolinPlayerState state) {
        state.setMode(BurrowMode.NONE);
        state.setRequestedMode(BurrowMode.NONE);
        state.setPhase(BurrowPhase.NONE);
        state.setPhaseTicks(0);
        state.setAirBoundaryTicks(0);
        state.setHelmetTier(getHelmetTier(player));
        state.setLastSafePos(currentPos(player));
        clearDigStateOnly(state);
    }

    private void clearDigStateOnly(PangolinPlayerState state) {
        state.clearDigState();
    }

    private void applyGlobalStateToOnlinePlayers() {
        if (server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PangolinPlayerState state = getState(player);
            state.setEnabled(config.globalEnabled);
            if (!config.globalEnabled) {
                forceEndBurrow(player, state);
            } else {
                syncPlayer(player, state);
            }
        }
    }

    private void ensureStarterHat(ServerPlayerEntity player) {
        if (hasStarterHat(player)) {
            return;
        }
        player.giveItemStack(PangolinItems.createStarterHat());
    }

    private boolean hasStarterHat(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (PangolinItems.isStarterHat(player.getInventory().getStack(slot))) {
                return true;
            }
        }
        return PangolinItems.isStarterHat(player.getEquippedStack(EquipmentSlot.HEAD));
    }

    private void recoverStamina(PangolinPlayerState state) {
        if (state.stamina() >= state.maxStamina()) {
            return;
        }
        state.setStamina(Math.min(state.maxStamina(), state.stamina() + config.staminaRecoveryPerTick));
    }

    private void rememberSafePos(ServerPlayerEntity player, PangolinPlayerState state) {
        BlockPos feet = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
        if (isTwoBlockAir(player.getEntityWorld(), feet)) {
            state.setLastSafePos(currentPos(player));
        }
    }

    private boolean consumePlacementStamina(ServerPlayerEntity player) {
        PangolinPlayerState state = getState(player);
        if (state.stamina() < config.blockPlaceCost) {
            sendHint(player, "hint.pangolinsurvival.low_stamina");
            return false;
        }

        state.setStamina(Math.max(0.0F, state.stamina() - config.blockPlaceCost));
        if (state.stamina() <= 0.0F) {
            downgradeToGhost(player, state, "hint.pangolinsurvival.low_stamina");
        }
        return true;
    }

    private boolean canInitiateBurrow(ServerPlayerEntity player) {
        if (findEntryMedium(player, 3) != null) {
            return true;
        }

        World world = player.getEntityWorld();
        for (BlockPos probe : getBoundaryProbes(player, true)) {
            if (isBurrowMedium(world.getBlockState(probe), world, probe)) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findEntryMedium(ServerPlayerEntity player, int searchDepth) {
        double sampleY = player.getY() - 0.2D;
        for (int offset = 0; offset <= searchDepth; offset++) {
            BlockPos candidate = BlockPos.ofFloored(player.getX(), sampleY - offset, player.getZ());
            if (isBurrowMedium(player.getEntityWorld().getBlockState(candidate), player.getEntityWorld(), candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isTwoBlockAir(World world, BlockPos feet) {
        return world.getBlockState(feet).isAir()
                && world.getBlockState(feet.up()).isAir()
                && world.getFluidState(feet).isEmpty()
                && world.getFluidState(feet.up()).isEmpty();
    }

    private boolean isBurrowMedium(BlockState state, World world, BlockPos pos) {
        return !state.isAir() && !isBurrowBlockedState(state, world, pos);
    }

    private boolean isBurrowBlockedState(BlockState state, World world, BlockPos pos) {
        return state.isIn(PangolinTags.HARD_BURROW_BAN)
                || state.hasBlockEntity()
                || !world.getFluidState(pos).isEmpty();
    }

    private HelmetBurrowTier getHelmetTier(PlayerEntity player) {
        return HelmetBurrowTier.fromHelmet(player.getEquippedStack(EquipmentSlot.HEAD));
    }

    private boolean isEnabledFor(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        if (player.getEntityWorld().isClient()) {
            return getClientState().enabled();
        }
        return config.globalEnabled;
    }

    private boolean isHoldingPickaxe(PlayerEntity player) {
        return player.getMainHandStack().isIn(ItemTags.PICKAXES);
    }

    private float getBreakCost(BlockState state, World world, BlockPos pos, HelmetBurrowTier tier) {
        float hardness = Math.max(0.0F, state.getHardness(world, pos));
        return Math.max(1.0F, (1.5F + hardness) * config.blockBreakStaminaMultiplier * tier.staminaMultiplier() * 0.1F);
    }

    private void relocateToLastSafePos(ServerPlayerEntity player, PangolinPlayerState state) {
        Vec3d lastSafePos = state.lastSafePos();
        if (lastSafePos == null) {
            return;
        }
        player.requestTeleport(lastSafePos.x, lastSafePos.y, lastSafePos.z);
        player.setVelocity(0.0D, 0.0D, 0.0D);
    }

    private PangolinClientState getClientState() {
        return PangolinSurvivalMod.getClientState();
    }

    private boolean shouldGrantStarterHatOnJoin(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)) == 0;
    }

    private void syncPlayer(ServerPlayerEntity player, PangolinPlayerState state) {
        boolean spectatorMotionEnabled = shouldEnableBurrowFlight(player, state);
        ServerPlayNetworking.send(player, new PangolinStatePayload(
                config.globalEnabled,
                state.mode().id(),
                state.phase().id(),
                state.stamina(),
                state.maxStamina(),
                state.cooldownTicks(),
                state.helmetTier().id(),
                state.digProgress(),
                spectatorMotionEnabled,
                isMovementNoclipActive(player),
                isBurrowMediumRequired(player)
        ));
        state.setDirty(false);
    }

    private void sendHint(ServerPlayerEntity player, String translationKey) {
        PangolinPlayerState state = getState(player);
        long now = player.getEntityWorld().getTimeOfDay();
        if (now - state.lastHintTick() < HINT_COOLDOWN_TICKS) {
            return;
        }
        state.setLastHintTick(now);
        player.sendMessage(Text.translatable(translationKey), true);
    }

    private Vec3d currentPos(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getY(), player.getZ());
    }

    private int resolvePlayerMaxStamina(UUID playerUuid) {
        Integer storedValue = config.playerMaxStamina.get(playerUuid.toString());
        if (storedValue == null) {
            return StaminaUpgradeRules.normalizeBaseMax(config.maxStamina);
        }
        return StaminaUpgradeRules.normalizeStoredMax(config.maxStamina, storedValue);
    }

    private void savePlayerMaxStamina(UUID playerUuid, int maxStamina) {
        config.playerMaxStamina.put(playerUuid.toString(), StaminaUpgradeRules.normalizeStoredMax(config.maxStamina, maxStamina));
        configManager.save(config);
    }
}

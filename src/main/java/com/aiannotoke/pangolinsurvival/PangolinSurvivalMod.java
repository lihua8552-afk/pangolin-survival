package com.aiannotoke.pangolinsurvival;

import com.aiannotoke.pangolinsurvival.command.PangolinCommands;
import com.aiannotoke.pangolinsurvival.config.ConfigManager;
import com.aiannotoke.pangolinsurvival.network.PangolinNetworking;
import com.aiannotoke.pangolinsurvival.service.PangolinService;
import com.aiannotoke.pangolinsurvival.state.PangolinClientState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PangolinSurvivalMod implements ModInitializer {

    public static final String MOD_ID = "pangolinsurvival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final PangolinClientState CLIENT_STATE = new PangolinClientState();
    private static ConfigManager configManager;
    private static PangolinService service;

    @Override
    public void onInitialize() {
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("pangolin-survival.json"));
        service = new PangolinService(configManager);
        PangolinNetworking.registerCommon(service);

        CommandRegistrationCallback.EVENT.register(PangolinCommands::register);
        ServerLifecycleEvents.SERVER_STARTED.register(service::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(service::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(service::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> service.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> service.onPlayerLeave(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register(service::onAfterRespawn);
        AttackBlockCallback.EVENT.register(service::onAttackBlock);
        AttackEntityCallback.EVENT.register(service::onAttackEntity);
        UseBlockCallback.EVENT.register(service::onUseBlock);
        UseItemCallback.EVENT.register(service::onUseItem);
        PlayerBlockBreakEvents.BEFORE.register(service::beforeBlockBreak);
        PlayerBlockBreakEvents.AFTER.register(service::afterBlockBreak);
        LOGGER.info("Pangolin Survival initialized");
    }

    public static PangolinService getService() {
        return service;
    }

    public static PangolinClientState getClientState() {
        return CLIENT_STATE;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }
}

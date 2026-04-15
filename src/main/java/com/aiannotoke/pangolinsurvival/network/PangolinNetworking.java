package com.aiannotoke.pangolinsurvival.network;

import com.aiannotoke.pangolinsurvival.service.PangolinService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PangolinNetworking {

    private static boolean commonRegistered;

    private PangolinNetworking() {
    }

    public static void registerCommon(PangolinService service) {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        PayloadTypeRegistry.playC2S().register(ToggleGhostBurrowPayload.ID, ToggleGhostBurrowPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleDigBurrowPayload.ID, ToggleDigBurrowPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpgradeStaminaPayload.ID, UpgradeStaminaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PangolinStatePayload.ID, PangolinStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleGhostBurrowPayload.ID, (payload, context) ->
                service.toggleGhost(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(ToggleDigBurrowPayload.ID, (payload, context) ->
                service.toggleDig(context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(UpgradeStaminaPayload.ID, (payload, context) ->
                service.upgradeMaxStamina(context.player())
        );
    }
}

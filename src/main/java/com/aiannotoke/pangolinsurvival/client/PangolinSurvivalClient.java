package com.aiannotoke.pangolinsurvival.client;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.network.PangolinStatePayload;
import com.aiannotoke.pangolinsurvival.network.ToggleDigBurrowPayload;
import com.aiannotoke.pangolinsurvival.network.ToggleGhostBurrowPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PangolinSurvivalClient implements ClientModInitializer {

    private static final PangolinHudState HUD_STATE = new PangolinHudState(PangolinSurvivalMod.getClientState());
    private static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(PangolinSurvivalMod.MOD_ID, "main"));
    private static KeyBinding toggleGhostKey;
    private static KeyBinding toggleDigKey;
    private static KeyBinding openUpgradeScreenKey;

    @Override
    public void onInitializeClient() {
        toggleGhostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pangolinsurvival.toggle_ghost",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY
        ));
        toggleDigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pangolinsurvival.toggle_dig",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY
        ));
        openUpgradeScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pangolinsurvival.open_stamina_upgrade",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY
        ));

        ClientPlayNetworking.registerGlobalReceiver(PangolinStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player == null) {
                        return;
                    }
                    PangolinSurvivalMod.getClientState().accept(payload);
                })
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PangolinSurvivalMod.getClientState().reset());

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HEALTH_BAR,
                Identifier.of(PangolinSurvivalMod.MOD_ID, "pangolin_hud"),
                HUD_STATE::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleGhostKey.wasPressed()) {
                if (ClientPlayNetworking.canSend(ToggleGhostBurrowPayload.ID)) {
                    ClientPlayNetworking.send(new ToggleGhostBurrowPayload());
                }
            }
            while (toggleDigKey.wasPressed()) {
                if (ClientPlayNetworking.canSend(ToggleDigBurrowPayload.ID)) {
                    ClientPlayNetworking.send(new ToggleDigBurrowPayload());
                }
            }
            while (openUpgradeScreenKey.wasPressed()) {
                if (client.player == null) {
                    break;
                }
                if (client.currentScreen instanceof StaminaUpgradeScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new StaminaUpgradeScreen(null));
                }
            }
        });
    }

    public static boolean isMovementNoclipActive() {
        return PangolinSurvivalMod.getClientState().movementNoclipActive();
    }

    public static boolean isBurrowMediumRequired() {
        return PangolinSurvivalMod.getClientState().burrowMediumRequired();
    }

    public static boolean isSpectatorMotionEnabled() {
        return PangolinSurvivalMod.getClientState().spectatorMotionEnabled();
    }
}

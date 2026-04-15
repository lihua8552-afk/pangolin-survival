package com.aiannotoke.pangolinsurvival.client;

import com.aiannotoke.pangolinsurvival.state.PangolinClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class PangolinHudState {

    private final PangolinClientState state;

    public PangolinHudState(PangolinClientState state) {
        this.state = state;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!state.enabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int x = 8;
        int y = context.getScaledWindowHeight() - 64;

        context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.title"), x, y, 0xFFF2D16B);
        y += 11;
        context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.mode", Text.translatable("mode.pangolinsurvival." + state.mode().id())), x, y, 0xFFFFFFFF);
        y += 11;
        context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.phase", Text.translatable("phase.pangolinsurvival." + state.phase().id())), x, y, 0xFF9DE1FF);
        y += 11;
        context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.stamina", Math.round(state.stamina()), state.maxStamina()), x, y, 0xFFB9F4A5);
        y += 11;
        context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.helmet", Text.translatable("helmet.pangolinsurvival." + state.helmetTier().id())), x, y, 0xFFD6D6D6);
        if (state.digProgressPercent() > 0.0F) {
            y += 11;
            context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.dig_progress", Math.round(state.digProgressPercent() * 100.0F)), x, y, 0xFFFFD166);
        }

        if (state.cooldownTicks() > 0) {
            y += 11;
            context.drawTextWithShadow(textRenderer, Text.translatable("hud.pangolinsurvival.cooldown", Math.max(1, state.cooldownTicks() / 20)), x, y, 0xFFFF9C6B);
        }
    }
}

package com.aiannotoke.pangolinsurvival.client;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.network.UpgradeStaminaPayload;
import com.aiannotoke.pangolinsurvival.util.StaminaUpgradeRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class StaminaUpgradeScreen extends Screen {

    private final Screen parent;
    private ButtonWidget upgradeButton;

    public StaminaUpgradeScreen(Screen parent) {
        super(Text.translatable("screen.pangolinsurvival.stamina_upgrade.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int panelTop = height / 2 - 58;

        upgradeButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> sendUpgradeRequest())
                .dimensions(centerX - 94, panelTop + 120, 188, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(centerX - 94, panelTop + 146, 188, 20)
                .build());

        refreshUpgradeButton();
    }

    @Override
    public void tick() {
        super.tick();
        if (client == null || client.player == null) {
            close();
            return;
        }
        refreshUpgradeButton();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderDarkening(context);

        if (client == null || client.player == null) {
            super.render(context, mouseX, mouseY, deltaTicks);
            return;
        }

        PlayerEntity player = client.player;
        int centerX = width / 2;
        int panelTop = height / 2 - 58;
        int panelLeft = centerX - 118;
        int panelRight = centerX + 118;
        int panelBottom = panelTop + 180;

        context.fillGradient(panelLeft, panelTop - 14, panelRight, panelBottom, 0xD81A1913, 0xC8141310);
        context.fill(panelLeft, panelTop - 14, panelRight, panelTop - 12, 0xB8D8C36A);
        context.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0x90B89D4D);
        context.fill(panelLeft, panelTop - 14, panelLeft + 2, panelBottom, 0xA0B89D4D);
        context.fill(panelRight - 2, panelTop - 14, panelRight, panelBottom, 0xA0B89D4D);

        int currentMax = PangolinSurvivalMod.getService().getObservedMaxStamina(player);
        int nextMax = PangolinSurvivalMod.getService().getNextUpgradeMaxStamina(player);
        int levelCost = PangolinSurvivalMod.getService().getUpgradeCostLevels(player);
        boolean capped = currentMax >= StaminaUpgradeRules.MAX_STAMINA_CAP;

        drawInfoRow(context, panelLeft + 16, panelTop + 22, panelRight - 16, Text.translatable("screen.pangolinsurvival.stamina_upgrade.current", currentMax), 0xFFFFFFFF);
        drawInfoRow(context, panelLeft + 16, panelTop + 48, panelRight - 16, Text.translatable("screen.pangolinsurvival.stamina_upgrade.experience", player.experienceLevel), 0xFFB9F4A5);

        if (capped) {
            drawInfoRow(context, panelLeft + 16, panelTop + 74, panelRight - 16, Text.translatable("screen.pangolinsurvival.stamina_upgrade.capped"), 0xFFFFC86B);
        } else {
            drawInfoRow(context, panelLeft + 16, panelTop + 74, panelRight - 16, Text.translatable("screen.pangolinsurvival.stamina_upgrade.next", nextMax), 0xFF9DE1FF);
            drawInfoRow(context, panelLeft + 16, panelTop + 100, panelRight - 16, Text.translatable("screen.pangolinsurvival.stamina_upgrade.cost", levelCost), 0xFFD6D6D6);
        }

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, panelTop, 0xFFF2D16B);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void refreshUpgradeButton() {
        if (upgradeButton == null || client == null || client.player == null) {
            return;
        }

        int currentMax = PangolinSurvivalMod.getService().getObservedMaxStamina(client.player);
        int nextMax = PangolinSurvivalMod.getService().getNextUpgradeMaxStamina(client.player);
        int levelCost = PangolinSurvivalMod.getService().getUpgradeCostLevels(client.player);
        boolean capped = currentMax >= StaminaUpgradeRules.MAX_STAMINA_CAP;

        if (capped) {
            upgradeButton.setMessage(Text.translatable("screen.pangolinsurvival.stamina_upgrade.button_capped"));
            upgradeButton.active = false;
            return;
        }

        upgradeButton.setMessage(Text.translatable("screen.pangolinsurvival.stamina_upgrade.button_upgrade", nextMax, levelCost));
        upgradeButton.active = client.player.experienceLevel >= levelCost;
    }

    private void sendUpgradeRequest() {
        if (ClientPlayNetworking.canSend(UpgradeStaminaPayload.ID)) {
            ClientPlayNetworking.send(new UpgradeStaminaPayload());
        }
    }

    private void drawInfoRow(DrawContext context, int left, int top, int right, Text text, int color) {
        context.fill(left - 8, top - 4, right, top + 14, 0x4A2A241B);
        context.fill(left - 8, top - 4, left - 6, top + 14, 0x90D8C36A);
        context.drawTextWithShadow(textRenderer, text, left, top, color);
    }
}

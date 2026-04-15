package com.aiannotoke.pangolinsurvival.command;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.aiannotoke.pangolinsurvival.state.HelmetBurrowTier;
import com.aiannotoke.pangolinsurvival.state.PangolinPlayerState;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class PangolinCommands {

    private PangolinCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("pangolin")
                .then(CommandManager.literal("global")
                        .requires(PangolinCommands::isGamemaster)
                        .then(CommandManager.literal("on").executes(context -> {
                            PangolinSurvivalMod.getService().setGlobalEnabled(true);
                            context.getSource().sendFeedback(() -> Text.translatable("command.pangolinsurvival.global_on"), true);
                            return 1;
                        }))
                        .then(CommandManager.literal("off").executes(context -> {
                            PangolinSurvivalMod.getService().setGlobalEnabled(false);
                            context.getSource().sendFeedback(() -> Text.translatable("command.pangolinsurvival.global_off"), true);
                            return 1;
                        })))
                .then(CommandManager.literal("reload")
                        .requires(PangolinCommands::isGamemaster)
                        .executes(context -> {
                            PangolinSurvivalMod.getService().reloadConfig();
                            context.getSource().sendFeedback(() -> Text.translatable("command.pangolinsurvival.reload"), false);
                            return 1;
                        }))
                .then(CommandManager.literal("status")
                        .executes(context -> showStatus(context.getSource()))));
    }

    private static int showStatus(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        PangolinPlayerState state = player == null ? null : PangolinSurvivalMod.getService().getState(player);
        HelmetBurrowTier tier = player == null ? HelmetBurrowTier.NONE : PangolinSurvivalMod.getService().getObservedHelmetTier(player);

        MutableText summary = Text.literal("[Pangolin] ")
                .append(Text.translatable(
                        "hud.pangolinsurvival.mode",
                        Text.translatable("mode.pangolinsurvival." + (state == null ? "none" : state.mode().id()))
                ))
                .append("  ")
                .append(Text.translatable(
                        "hud.pangolinsurvival.stamina",
                        state == null ? 0 : Math.round(state.stamina()),
                        state == null ? PangolinSurvivalMod.getService().getConfig().maxStamina : state.maxStamina()
                ))
                .append("  ")
                .append(Text.translatable("hud.pangolinsurvival.helmet", Text.translatable("helmet.pangolinsurvival." + tier.id())));

        if (isGamemaster(source)) {
            summary = summary.append(Text.literal("  "))
                    .append(Text.literal("Global: "))
                    .append(Text.translatable(PangolinSurvivalMod.getService().getConfig().globalEnabled
                            ? "common.pangolinsurvival.enabled"
                            : "common.pangolinsurvival.disabled"));
        }

        MutableText finalSummary = summary;
        source.sendFeedback(() -> finalSummary, false);
        return 1;
    }

    private static boolean isGamemaster(ServerCommandSource source) {
        return CommandManager.GAMEMASTERS_CHECK.allows(source.getPermissions());
    }
}

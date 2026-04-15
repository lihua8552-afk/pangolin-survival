package com.aiannotoke.pangolinsurvival.config;

import com.aiannotoke.pangolinsurvival.PangolinSurvivalMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Path configPath;
    private final Gson gson;

    public ConfigManager(Path configPath) {
        this.configPath = configPath;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public synchronized PangolinConfig loadOrCreate() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            if (!Files.exists(configPath)) {
                PangolinConfig config = new PangolinConfig();
                config.fillDefaults();
                save(config);
                return config;
            }

            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                PangolinConfig config = gson.fromJson(reader, PangolinConfig.class);
                if (config == null) {
                    config = new PangolinConfig();
                }
                if (config.fillDefaults()) {
                    save(config);
                }
                return config;
            }
        } catch (Exception exception) {
            PangolinSurvivalMod.LOGGER.error("Failed to load config {}, using defaults", configPath, exception);
            PangolinConfig fallback = new PangolinConfig();
            fallback.fillDefaults();
            return fallback;
        }
    }

    public synchronized PangolinConfig reload() {
        return loadOrCreate();
    }

    public synchronized void save(PangolinConfig config) {
        config.fillDefaults();
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
        } catch (IOException exception) {
            PangolinSurvivalMod.LOGGER.error("Failed to save config {}", configPath, exception);
        }
    }
}

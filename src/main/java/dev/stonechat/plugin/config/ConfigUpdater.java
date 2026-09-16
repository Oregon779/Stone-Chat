package dev.stonechat.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    public static YamlConfiguration updateFile(Plugin plugin, File file, String resourcePath) {
        boolean isNewFile = !file.exists();

        if (isNewFile) {
            plugin.saveResource(resourcePath, false);
            return YamlConfiguration.loadConfiguration(file);
        }

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaultConfig = loadDefault(plugin, resourcePath);

        if (defaultConfig == null) {
            return userConfig;
        }

        boolean changed = mergeMissingKeys(defaultConfig, userConfig);

        if (changed) {
            try {
                userConfig.save(file);
                plugin.getLogger().info("Added missing options to " + file.getName() + " (existing values were kept untouched).");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not save updated " + file.getName(), e);
            }
        }

        return userConfig;
    }

    private static YamlConfiguration loadDefault(Plugin plugin, String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled default for " + resourcePath, e);
            return null;
        }
    }

    private static boolean mergeMissingKeys(ConfigurationSection source, ConfigurationSection target) {
        boolean changed = false;

        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);

            if (sourceValue instanceof ConfigurationSection sourceSection) {
                if (!target.isConfigurationSection(key)) {

                    target.set(key, null);
                    ConfigurationSection newSection = target.createSection(key);
                    changed |= mergeMissingKeys(sourceSection, newSection);
                } else {

                    changed |= mergeMissingKeys(sourceSection, target.getConfigurationSection(key));
                }
            } else if (!target.contains(key)) {
                target.set(key, sourceValue);
                changed = true;
            }

        }

        return changed;
    }
}

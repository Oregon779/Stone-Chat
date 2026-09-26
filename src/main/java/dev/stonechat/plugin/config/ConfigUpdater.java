package dev.stonechat.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.logging.Level;

public final class ConfigUpdater {

    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigUpdater() {
    }

    public static YamlConfiguration updateFile(Plugin plugin, File file, String resourcePath) {
        return updateFile(plugin, file, resourcePath, Set.of());
    }

    /**
     * Adds options missing from {@code file} (compared to the bundled default) without touching existing values.
     *
     * @param userManagedSections dotted paths of sections whose entries the admin adds/removes freely (e.g. the
     *                            list of chat colors). They are only created when missing entirely, never refilled.
     */
    public static YamlConfiguration updateFile(Plugin plugin, File file, String resourcePath, Set<String> userManagedSections) {
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
            return YamlConfiguration.loadConfiguration(file);
        }

        YamlConfiguration defaultConfig = loadDefault(plugin, resourcePath);
        YamlConfiguration userConfig = new YamlConfiguration();
        try {
            userConfig.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            // Merging into (and saving) the empty result would overwrite the admin's whole file with defaults.
            Path backup = backupBrokenFile(plugin, file);
            plugin.getLogger().severe(file.getName() + " could not be read and is ignored until it is fixed: " + e.getMessage());
            plugin.getLogger().severe("Using the built-in defaults for " + file.getName() + " for now."
                    + (backup != null ? " An untouched copy was saved as " + backup.getFileName() + "." : ""));
            return defaultConfig != null ? defaultConfig : new YamlConfiguration();
        }

        if (defaultConfig == null) {
            return userConfig;
        }

        boolean changed = mergeMissingKeys(defaultConfig, userConfig, "", userManagedSections);

        if (changed) {
            try {
                dev.stonechat.plugin.util.DataFiles.writeAtomically(file.toPath(), userConfig.saveToString());
                plugin.getLogger().info("Added missing options to " + file.getName() + " (existing values were kept untouched).");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not save updated " + file.getName(), e);
            }
        }

        return userConfig;
    }

    private static Path backupBrokenFile(Plugin plugin, File file) {
        Path source = file.toPath();
        Path backup = source.resolveSibling(file.getName() + ".broken-" + LocalDateTime.now().format(BACKUP_STAMP));
        try {
            return Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not back up broken " + file.getName(), e);
            return null;
        }
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

    private static boolean mergeMissingKeys(ConfigurationSection source, ConfigurationSection target,
                                            String parentPath, Set<String> userManagedSections) {
        boolean changed = false;

        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);
            String path = parentPath.isEmpty() ? key : parentPath + "." + key;

            if (sourceValue instanceof ConfigurationSection sourceSection) {
                if (!target.isConfigurationSection(key)) {
                    target.set(key, null);
                    ConfigurationSection newSection = target.createSection(key);
                    mergeMissingKeys(sourceSection, newSection, path, Set.of());
                    changed = true;
                } else if (!userManagedSections.contains(path)) {
                    changed |= mergeMissingKeys(sourceSection, target.getConfigurationSection(key), path, userManagedSections);
                }
            } else if (!target.contains(key)) {
                target.set(key, sourceValue);
                changed = true;
            }
        }

        return changed;
    }
}

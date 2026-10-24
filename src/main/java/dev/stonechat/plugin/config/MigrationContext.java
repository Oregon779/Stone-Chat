package dev.stonechat.plugin.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.function.Consumer;

/**
 * The operations a migration step may perform on an admin's file. Every operation is idempotent (running it twice
 * changes nothing the second time) and records what it changed, so the console can tell the admin exactly what
 * happened to their file.
 */
public final class MigrationContext {

    private final ConfigurationSection config;
    private final List<String> changes;

    MigrationContext(ConfigurationSection config, List<String> changes) {
        this.config = config;
        this.changes = changes;
    }

    /** Direct access for migrations the helpers below don't cover. Record what you change with {@link #note}. */
    public ConfigurationSection config() {
        return config;
    }

    public void note(String change) {
        changes.add(change);
    }

    /** Deletes an option (or a whole section) that this plugin version no longer uses. */
    public void remove(String path) {
        if (!config.contains(path)) return;
        config.set(path, null);
        changes.add("removed " + path);
    }

    /**
     * Renames or moves an option, keeping the admin's value and the comments above it. If the new path already
     * exists, that value wins and the old option is only removed.
     */
    public void move(String from, String to) {
        if (!config.contains(from)) return;
        if (config.contains(to)) {
            changes.add("removed " + from + " (" + to + " is already set)");
        } else {
            Object value = config.get(from);
            if (value instanceof ConfigurationSection section) {
                ConfigUpdater.copySection(section, config.createSection(to));
            } else {
                config.set(to, value);
            }
            config.setComments(to, config.getComments(from));
            config.setInlineComments(to, config.getInlineComments(from));
            changes.add("moved " + from + " to " + to);
        }
        config.set(from, null);
    }

    /**
     * Replaces a text value only where it still equals {@code oldValue} (ignoring case), e.g. a name Minecraft
     * renamed. Values the admin chose themselves are left alone.
     */
    public void replaceValue(String path, String oldValue, String newValue) {
        if (config.get(path) instanceof String current && current.trim().equalsIgnoreCase(oldValue)) {
            config.set(path, newValue);
            changes.add(path + ": " + current + " -> " + newValue);
        }
    }

    /** Runs {@code action} with the full path of every entry directly below {@code sectionPath}, e.g. every game. */
    public void forEachChild(String sectionPath, Consumer<String> action) {
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        if (section == null) return;
        for (String key : List.copyOf(section.getKeys(false))) {
            action.accept(sectionPath + "." + key);
        }
    }
}

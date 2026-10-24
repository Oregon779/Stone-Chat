package dev.stonechat.plugin.config;

import dev.stonechat.plugin.util.DataFiles;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps the admin's YAML files in step with the bundled defaults of the running plugin version. Every loader calls
 * {@link #updateFile} before it reads a single value, on startup and on /stonechat reload.
 * <ul>
 *     <li>Options the bundled default has and the file doesn't are added with their default value and comments,
 *     at the same place as in the default - also deep inside nested sections.</li>
 *     <li>Values, comments, order and extra keys of the admin are never changed. Only versioned migrations
 *     ({@link ConfigMigrations}) may rename or remove options.</li>
 *     <li>A file is only rewritten when something actually changed, and the previous file is kept as
 *     {@code <name>.bak} first.</li>
 * </ul>
 * Comments survive because Paper's YamlConfiguration reads and writes them. Rewriting normalizes YAML style
 * (quotes, list indentation) the same way the in-game editor does when it saves.
 */
public final class ConfigUpdater {

    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> VERSION_KEYS = List.of("config-version", "messages-version");
    private static final int MAX_LOGGED_PATHS = 15;

    private ConfigUpdater() {
    }

    /** What {@link #update} did to a file. {@code config} is the admin's own object when nothing changed. */
    record Result(YamlConfiguration config, List<String> migrated, List<String> added, List<String> warnings) {

        boolean changed() {
            return !migrated.isEmpty() || !added.isEmpty();
        }
    }

    public static YamlConfiguration updateFile(Plugin plugin, File file, String resourcePath) {
        return updateFile(plugin, file, resourcePath, Set.of());
    }

    /**
     * Brings {@code file} up to date with the bundled default at {@code resourcePath} and returns its content.
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

        Logger logger = plugin.getLogger();
        Result result;
        try {
            result = update(userConfig, defaultConfig, ConfigMigrations.stepsFor(resourcePath), userManagedSections);
        } catch (RuntimeException e) {
            // A faulty migration step must neither stop the plugin nor leave a half-migrated file behind.
            logger.log(Level.SEVERE, "Could not update " + file.getName() + ", it is used as it is and was not changed", e);
            return YamlConfiguration.loadConfiguration(file);
        }
        result.warnings().forEach(warning -> logger.warning(file.getName() + ": " + warning));
        if (result.changed()) {
            save(logger, file, result);
        }
        return result.config();
    }

    /**
     * Migrates {@code user} to the version of {@code defaults} and adds every option it is missing. Pure: no file
     * access, so it can be tested with plain YAML strings.
     */
    static Result update(YamlConfiguration user, YamlConfiguration defaults,
                         Map<Integer, Consumer<MigrationContext>> migrations, Set<String> userManagedSections) {
        List<String> warnings = new ArrayList<>();
        String versionKey = VERSION_KEYS.stream().filter(defaults::isInt).findFirst().orElse(null);
        List<String> migrated = migrate(user, defaults, versionKey, migrations, warnings);

        Merge merge = new Merge(userManagedSections, versionKey, warnings);
        YamlConfiguration merged = merge.apply(user, defaults);

        boolean changed = !migrated.isEmpty() || !merge.added.isEmpty();
        return new Result(changed ? merged : user, migrated, merge.added, warnings);
    }

    private static List<String> migrate(YamlConfiguration user, YamlConfiguration defaults, String versionKey,
                                        Map<Integer, Consumer<MigrationContext>> migrations, List<String> warnings) {
        if (versionKey == null) return List.of();

        int target = defaults.getInt(versionKey);
        // A file without a version predates versioning (or the line was deleted): run every step, they are idempotent.
        int current = user.isInt(versionKey) ? user.getInt(versionKey) : 1;
        if (current > target) {
            warnings.add("written by a newer Stone Chat version (" + versionKey + " " + current + ", this version knows "
                    + target + "). Nothing is migrated or removed, only missing options are added.");
            return List.of();
        }
        if (current == target) return List.of();

        List<String> changes = new ArrayList<>();
        MigrationContext context = new MigrationContext(user, changes);
        for (int version = current; version < target; version++) {
            Consumer<MigrationContext> step = migrations.get(version);
            if (step != null) step.accept(context);
        }
        if (user.contains(versionKey)) {
            user.set(versionKey, target);
        }
        changes.add(versionKey + " " + current + " -> " + target);
        return changes;
    }

    /** Rebuilds the admin's file key by key, slotting missing default options in where the default has them. */
    private static final class Merge {

        private final Set<String> userManagedSections;
        private final String versionKey;
        private final List<String> warnings;
        private final List<String> added = new ArrayList<>();

        Merge(Set<String> userManagedSections, String versionKey, List<String> warnings) {
            this.userManagedSections = userManagedSections;
            this.versionKey = versionKey;
            this.warnings = warnings;
        }

        YamlConfiguration apply(YamlConfiguration user, YamlConfiguration defaults) {
            YamlConfiguration result = new YamlConfiguration();
            boolean emptyFile = user.getKeys(false).isEmpty();
            result.options().setHeader(emptyFile ? defaults.options().getHeader() : user.options().getHeader());
            result.options().setFooter(user.options().getFooter());
            mergeSection(user, defaults, result, "");
            return result;
        }

        private void mergeSection(ConfigurationSection user, ConfigurationSection defaults, ConfigurationSection out, String parentPath) {
            for (String key : mergedKeyOrder(user.getKeys(false), defaults.getKeys(false))) {
                String path = parentPath.isEmpty() ? key : parentPath + "." + key;

                if (!user.contains(key)) {
                    if (defaults.get(key) instanceof ConfigurationSection defaultSection) {
                        copySection(defaultSection, out.createSection(key));
                    } else {
                        out.set(key, defaults.get(key));
                    }
                    // In the bundled files the file header sits directly above the version key. The admin's file
                    // already has that header, so a re-added version key must not bring a second copy along.
                    if (!path.equals(versionKey)) {
                        copyComments(defaults, out, key);
                    }
                    added.add(path);
                    continue;
                }

                Object userValue = user.get(key);
                Object defaultValue = defaults.get(key);
                if (userValue instanceof ConfigurationSection userSection) {
                    ConfigurationSection outSection = out.createSection(key);
                    if (defaultValue instanceof ConfigurationSection defaultSection && !userManagedSections.contains(path)) {
                        mergeSection(userSection, defaultSection, outSection, path);
                    } else {
                        if (defaultValue != null && !(defaultValue instanceof ConfigurationSection)) {
                            typeConflict(path, "a single value", "a section");
                        }
                        copySection(userSection, outSection);
                    }
                } else {
                    if (defaultValue instanceof ConfigurationSection) {
                        typeConflict(path, "a section", "a single value");
                    }
                    out.set(key, userValue);
                }
                copyComments(user, out, key);
            }
        }

        private void typeConflict(String path, String expected, String found) {
            warnings.add("'" + path + "' should be " + expected + " in this version but is " + found
                    + ". Your value was kept unchanged - please check this option.");
        }
    }

    /**
     * The admin's keys in the admin's order, plus every missing default key placed right after the default key
     * that precedes it (or before the one that follows it), so new options appear next to related ones.
     */
    static List<String> mergedKeyOrder(Collection<String> userKeys, Collection<String> defaultKeys) {
        List<String> order = new ArrayList<>(userKeys);
        List<String> defaults = List.copyOf(defaultKeys);
        for (int i = 0; i < defaults.size(); i++) {
            String key = defaults.get(i);
            if (order.contains(key)) continue;

            int insertAt = -1;
            for (int before = i - 1; before >= 0 && insertAt < 0; before--) {
                int index = order.indexOf(defaults.get(before));
                if (index >= 0) insertAt = index + 1;
            }
            for (int after = i + 1; after < defaults.size() && insertAt < 0; after++) {
                insertAt = order.indexOf(defaults.get(after));
            }
            order.add(insertAt < 0 ? order.size() : insertAt, key);
        }
        return order;
    }

    /** Deep copy of a section including the comments of every entry. */
    static void copySection(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection child) {
                copySection(child, to.createSection(key));
            } else {
                to.set(key, value);
            }
            copyComments(from, to, key);
        }
    }

    private static void copyComments(ConfigurationSection from, ConfigurationSection to, String key) {
        to.setComments(key, from.getComments(key));
        to.setInlineComments(key, from.getInlineComments(key));
    }

    private static void save(Logger logger, File file, Result result) {
        Path path = file.toPath();
        Path backup = path.resolveSibling(file.getName() + ".bak");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Without a copy of the old file nothing is written; the updated values still apply until the next start.
            logger.log(Level.WARNING, "Could not back up " + file.getName() + ", so it was not updated on disk", e);
            return;
        }
        try {
            DataFiles.writeAtomically(path, result.config().saveToString());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save updated " + file.getName(), e);
            return;
        }

        if (!result.migrated().isEmpty()) {
            logger.info("Migrated " + file.getName() + ": " + summarize(result.migrated()));
        }
        if (!result.added().isEmpty()) {
            logger.info("Added " + result.added().size() + " new option(s) to " + file.getName() + ": "
                    + summarize(result.added()) + ". Your existing values were kept.");
        }
        logger.info("The previous " + file.getName() + " was saved as " + backup.getFileName() + ".");
    }

    private static String summarize(List<String> entries) {
        if (entries.size() <= MAX_LOGGED_PATHS) return String.join(", ", entries);
        return String.join(", ", entries.subList(0, MAX_LOGGED_PATHS)) + " and " + (entries.size() - MAX_LOGGED_PATHS) + " more";
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
}

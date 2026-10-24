package dev.stonechat.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** The merge and migration rules on plain YAML text, without a server. */
class ConfigMergeTest {

    private static final String DEFAULTS = """
            # My Plugin
            config-version: 2
            features:
              example:
                # Turns the example feature on or off.
                enabled: true
                # Seconds between two uses.
                cooldown-seconds: 5
              other:
                # A feature added in a later version.
                enabled: true
            messages:
              prefix: '[Plugin] '
            """;

    private static final String OLD_USER_FILE = """
            # My Plugin
            config-version: 2
            features:
              example:
                # Turns the example feature on or off.
                enabled: false # switched off on purpose
            messages:
              # Our server name instead of the plugin's
              prefix: '[MyServer] '
            my-own-note: 42
            """;

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new AssertionError(e);
        }
        return yaml;
    }

    private static String bundledText(String resource) {
        try (InputStream in = ConfigMergeTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static ConfigUpdater.Result update(String user, String defaults) {
        return ConfigUpdater.update(yaml(user), yaml(defaults), Map.of(), Set.of());
    }

    @Test
    void newNestedOptionsAreAddedWhereTheDefaultHasThemAndExistingValuesStay() {
        ConfigUpdater.Result result = update(OLD_USER_FILE, DEFAULTS);

        assertEquals(List.of("features.example.cooldown-seconds", "features.other"), result.added());
        assertEquals("""
                # My Plugin
                config-version: 2
                features:
                  example:
                    # Turns the example feature on or off.
                    enabled: false # switched off on purpose
                    # Seconds between two uses.
                    cooldown-seconds: 5
                  other:
                    # A feature added in a later version.
                    enabled: true
                messages:
                  # Our server name instead of the plugin's
                  prefix: '[MyServer] '
                my-own-note: 42
                """, result.config().saveToString());
    }

    @Test
    void anUpToDateFileIsLeftAlone() {
        YamlConfiguration user = yaml(DEFAULTS);
        ConfigUpdater.Result result = ConfigUpdater.update(user, yaml(DEFAULTS), Map.of(), Set.of());

        assertFalse(result.changed());
        assertSame(user, result.config(), "nothing to rewrite");
    }

    @Test
    void aValueOfTheWrongTypeIsKeptAndReported() {
        ConfigUpdater.Result result = update("""
                config-version: 2
                features: disabled
                messages:
                  prefix:
                    text: custom
                """, DEFAULTS);

        assertEquals("disabled", result.config().get("features"));
        assertEquals("custom", result.config().getString("messages.prefix.text"));
        assertEquals(2, result.warnings().size(), result.warnings().toString());
        assertTrue(result.warnings().get(0).contains("'features'"));
    }

    @Test
    void userManagedSectionsAreCreatedWhenMissingButNeverRefilled() {
        String defaults = """
                colors:
                  red: '&c'
                  blue: '&9'
                """;
        Set<String> managed = Set.of("colors");

        ConfigUpdater.Result removedOne = ConfigUpdater.update(yaml("colors:\n  red: '&c'\n"), yaml(defaults), Map.of(), managed);
        assertFalse(removedOne.changed(), "the admin deleted blue on purpose");

        ConfigUpdater.Result missing = ConfigUpdater.update(yaml("other: 1\n"), yaml(defaults), Map.of(), managed);
        assertEquals(Set.of("red", "blue"), missing.config().getConfigurationSection("colors").getKeys(false));
    }

    @Test
    void migrationsRunInOrderFromTheFilesVersionAndKeepTheAdminsValue() {
        String defaults = """
                config-version: 3
                features:
                  demo:
                    enabled: true
                """;
        String user = """
                config-version: 1
                features:
                  example:
                    # Admin comment that must move along
                    enabled: false
                legacy-option: x
                """;
        List<Integer> ran = new ArrayList<>();
        Map<Integer, Consumer<MigrationContext>> steps = Map.of(
                1, m -> { ran.add(1); m.move("features.example", "features.demo"); },
                2, m -> { ran.add(2); m.remove("legacy-option"); },
                3, m -> ran.add(3));

        ConfigUpdater.Result result = ConfigUpdater.update(yaml(user), yaml(defaults), steps, Set.of());

        assertEquals(List.of(1, 2), ran, "step 3 belongs to a future version");
        assertEquals(List.of("moved features.example to features.demo", "removed legacy-option", "config-version 1 -> 3"),
                result.migrated());
        assertEquals(List.of(), result.added(), "the moved section already satisfies the new default");
        assertEquals("""
                config-version: 3
                features:
                  demo:
                    # Admin comment that must move along
                    enabled: false
                """, result.config().saveToString());
    }

    @Test
    void aFileFromANewerPluginVersionIsNotMigratedButStillCompleted() {
        String user = "config-version: 7\nfeatures:\n  example:\n    enabled: false\n";
        Map<Integer, Consumer<MigrationContext>> steps = Map.of(2, m -> fail("must not migrate a newer file"));

        ConfigUpdater.Result result = ConfigUpdater.update(yaml(user), yaml(DEFAULTS), steps, Set.of());

        assertEquals(7, result.config().getInt("config-version"));
        assertTrue(result.migrated().isEmpty());
        assertTrue(result.config().contains("features.other.enabled"));
        assertTrue(result.warnings().get(0).contains("newer Stone Chat version"));
    }

    @Test
    void migrationHelpersAreIdempotent() {
        YamlConfiguration config = yaml("old: 1\nparticle: totem\nkeep: HEART\ngames:\n  a:\n    p: TOTEM\n  b:\n    p: x\n");
        Consumer<MigrationContext> step = m -> {
            m.move("old", "new");
            m.remove("gone");
            m.replaceValue("particle", "TOTEM", "TOTEM_OF_UNDYING");
            m.replaceValue("keep", "TOTEM", "TOTEM_OF_UNDYING");
            m.forEachChild("games", game -> m.replaceValue(game + ".p", "TOTEM", "TOTEM_OF_UNDYING"));
        };

        List<String> first = new ArrayList<>();
        step.accept(new MigrationContext(config, first));
        List<String> second = new ArrayList<>();
        step.accept(new MigrationContext(config, second));

        assertEquals(3, first.size(), first.toString());
        assertEquals(List.of(), second, "running a step again changes nothing");
        assertEquals(1, config.getInt("new"));
        assertEquals("TOTEM_OF_UNDYING", config.getString("games.a.p"));
        assertEquals("x", config.getString("games.b.p"));
        assertEquals("HEART", config.getString("keep"));
    }

    @Test
    void aReAddedVersionKeyDoesNotDuplicateTheHeader() {
        String bundled = bundledText("config.yml");
        String withoutVersionLine = bundled.replaceFirst("(?m)^config-version: \\d+\\n", "");

        ConfigUpdater.Result result = ConfigUpdater.update(yaml(withoutVersionLine), yaml(bundled), Map.of(), Set.of());

        String saved = result.config().saveToString();
        assertEquals(List.of("config-version"), result.added());
        assertEquals(1, saved.split("Stone Chat - config.yml", -1).length - 1, "header exactly once");
    }

    @Test
    void everyBundledFileRestoresAnyMissingOptionExactlyAsShipped() {
        Map<String, Set<String>> files = Map.of(
                "config.yml", Set.of("chat-color-gui.colors"),
                "chatgames.yml", Set.of("custom-games"),
                "chatformat.yml", Set.of(),
                "languages/en/messages.yml", Set.of(),
                "languages/de/messages.yml", Set.of(),
                "gui/chatcolor-gui.yml", Set.of(),
                "gui/settings-gui.yml", Set.of());

        int checked = 0;
        for (var file : files.entrySet()) {
            String bundled = bundledText(file.getKey());
            String expected = yaml(bundled).saveToString();
            for (String path : pathsUpToDepth(yaml(bundled), 2)) {
                if (path.endsWith("-version") || isInside(path, file.getValue())) continue;

                YamlConfiguration user = yaml(bundled);
                user.set(path, null);
                ConfigUpdater.Result result = ConfigUpdater.update(user, yaml(bundled), Map.of(), file.getValue());

                assertEquals(List.of(path), result.added(), file.getKey());
                assertEquals(expected, result.config().saveToString(), file.getKey() + " without " + path);
                checked++;
            }
        }
        assertTrue(checked > 300, "checked " + checked + " options");
    }

    @Test
    void migrationStepsMatchTheVersionsOfTheBundledFiles() {
        for (var entry : ConfigMigrations.all().entrySet()) {
            YamlConfiguration bundled = yaml(bundledText(entry.getKey()));
            String versionKey = bundled.isInt("config-version") ? "config-version" : "messages-version";
            assertTrue(bundled.isInt(versionKey), entry.getKey() + " needs a version number");
            int shipped = bundled.getInt(versionKey);
            for (int from : entry.getValue().keySet()) {
                assertTrue(from >= 1 && from < shipped,
                        entry.getKey() + ": step " + from + " never runs, raise " + versionKey + " to " + (from + 1));
            }
        }
    }

    @Test
    void newKeysGoNextToTheirNeighboursFromTheDefault() {
        assertEquals(List.of("a", "b", "mine", "c"), ConfigUpdater.mergedKeyOrder(List.of("a", "mine", "c"), List.of("a", "b", "c")));
        assertEquals(List.of("x", "a", "mine"), ConfigUpdater.mergedKeyOrder(List.of("a", "mine"), List.of("x", "a")));
        assertEquals(List.of("mine", "a", "b"), ConfigUpdater.mergedKeyOrder(List.of("mine"), List.of("a", "b")));
    }

    private static List<String> pathsUpToDepth(ConfigurationSection section, int depth) {
        List<String> paths = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String path = section.getCurrentPath().isEmpty() ? key : section.getCurrentPath() + "." + key;
            paths.add(path);
            if (depth > 1 && section.get(key) instanceof ConfigurationSection child) {
                paths.addAll(pathsUpToDepth(child, depth - 1));
            }
        }
        return paths;
    }

    private static boolean isInside(String path, Set<String> sections) {
        return sections.stream().anyMatch(section -> path.startsWith(section + "."));
    }
}

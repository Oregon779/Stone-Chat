package dev.stonechat.plugin.config;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Versioned migrations for the bundled YAML files.
 * <p>
 * <b>New options need no entry here.</b> {@link ConfigUpdater} adds every option of the bundled default to the
 * admin's file automatically, with its default value and comments. A migration is only needed when an existing
 * option is renamed, moved, removed or changes its meaning, because adding alone would leave the old option behind
 * and silently drop the admin's setting.
 * <p>
 * To add one:
 * <ol>
 *     <li>raise {@code config-version} (or {@code messages-version}) in the bundled default file by one,</li>
 *     <li>register a step below under the <b>old</b> version number. A file on version 1 runs step 1, then step 2,
 *     and so on up to the bundled version.</li>
 * </ol>
 * Steps must be idempotent; the helpers in {@link MigrationContext} already are. Each file is backed up before a
 * step runs. Files without a version number (gui/*.yml) become migratable by adding {@code config-version: 1} to
 * their bundled default.
 */
public final class ConfigMigrations {

    private static final Map<String, Map<Integer, Consumer<MigrationContext>>> STEPS = Map.of(
            "config.yml", Map.of(
                    // The editor before 1.2 wrote these two, but nothing ever read them: the chat color GUI
                    // takes its size and titles from gui/chatcolor-gui.yml.
                    1, migration -> {
                        migration.remove("chat-color-gui.size");
                        migration.remove("chat-color-gui.title");
                    }),
            "chatgames.yml", Map.of(
                    // Minecraft renamed the TOTEM particle to TOTEM_OF_UNDYING, older files still ship the old name.
                    2, migration -> {
                        migration.replaceValue("chat-games.default-reward.particle.name", "TOTEM", "TOTEM_OF_UNDYING");
                        for (String games : new String[]{"default-games", "custom-games"}) {
                            migration.forEachChild(games, game ->
                                    migration.replaceValue(game + ".reward.particle.name", "TOTEM", "TOTEM_OF_UNDYING"));
                        }
                    })
    );

    private ConfigMigrations() {
    }

    /** Steps for a bundled file, keyed by the version they upgrade from. */
    static Map<Integer, Consumer<MigrationContext>> stepsFor(String resourcePath) {
        return STEPS.getOrDefault(resourcePath, Map.of());
    }

    static Map<String, Map<Integer, Consumer<MigrationContext>>> all() {
        return STEPS;
    }
}

package dev.stonechat.plugin.config;

import dev.stonechat.plugin.PluginTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUpdaterTest extends PluginTestBase {

    private Path configFile() {
        return new File(plugin.getDataFolder(), "config.yml").toPath();
    }

    @Test
    void brokenYamlIsNeverOverwrittenWithDefaults() throws Exception {
        String broken = "cooldown:\n  chat-seconds: 42\n   command-seconds: 3\n";
        Files.writeString(configFile(), broken, StandardCharsets.UTF_8);

        plugin.reload();

        assertEquals(broken, Files.readString(configFile()), "the admin's file must stay exactly as it was");
        assertEquals(5, plugin.getConfigManager().getChatCooldownSeconds(), "plugin keeps running on defaults");
        try (var files = Files.list(plugin.getDataFolder().toPath())) {
            List<Path> backups = files.filter(p -> p.getFileName().toString().startsWith("config.yml.broken-")).toList();
            assertEquals(1, backups.size());
            assertEquals(broken, Files.readString(backups.get(0)));
        }
    }

    @Test
    void removedChatColorsStayRemovedButMissingOptionsAreRestored() throws Exception {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile().toFile());
        assertTrue(yaml.contains("chat-color-gui.colors.rainbow"));
        yaml.set("chat-color-gui.colors.rainbow", null);
        yaml.set("cooldown.chat-seconds", null);
        yaml.save(configFile().toFile());

        plugin.reload();

        YamlConfiguration after = YamlConfiguration.loadConfiguration(configFile().toFile());
        assertFalse(after.contains("chat-color-gui.colors.rainbow"), "admin removed this color on purpose");
        assertTrue(after.contains("chat-color-gui.colors.gold"), "other colors are untouched");
        assertEquals(5, after.getInt("cooldown.chat-seconds"), "a normal missing option is added back");
    }

    @Test
    void missingUserManagedSectionIsCreatedWithDefaults() throws Exception {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile().toFile());
        yaml.set("chat-color-gui", null);
        yaml.save(configFile().toFile());

        YamlConfiguration merged = ConfigUpdater.updateFile(plugin, configFile().toFile(), "config.yml",
                Set.of("chat-color-gui.colors"));

        assertTrue(merged.contains("chat-color-gui.colors.rainbow"));
        assertTrue(merged.contains("chat-color-gui.colors.default"));
    }

    @Test
    void anOldConfigIsMigratedCompletedAndBackedUpBeforeItIsRead() throws Exception {
        YamlConfiguration old = YamlConfiguration.loadConfiguration(configFile().toFile());
        old.set("config-version", 1);
        old.set("chat-color-gui.size", 27);              // written by the editor before 1.2, never read
        old.set("chat-color-gui.title", "Colors");
        old.set("cooldown.chat-seconds", 9);
        old.setComments("cooldown.chat-seconds", List.of("Raised after the spam wave in May"));
        old.set("auto-messages", null);                  // a whole section that version did not have yet
        old.set("chat-log.retention-days", null);        // and a single nested option
        old.save(configFile().toFile());
        String oldText = Files.readString(configFile());

        plugin.reload();

        String newText = Files.readString(configFile());
        YamlConfiguration now = YamlConfiguration.loadConfiguration(configFile().toFile());
        assertEquals(2, now.getInt("config-version"));
        assertFalse(now.contains("chat-color-gui.size"));
        assertFalse(now.contains("chat-color-gui.title"));
        assertEquals(9, now.getInt("cooldown.chat-seconds"), "the admin's value stays");
        assertEquals(List.of("Raised after the spam wave in May"), now.getComments("cooldown.chat-seconds"));
        assertEquals(30, now.getInt("chat-log.retention-days"), "new options get the default of this version");
        assertTrue(now.getComments("auto-messages").contains(
                "Automatic messages: every interval-seconds, one of the messages below is"), "with its documentation");
        assertTrue(newText.indexOf("\nchat-log:") < newText.indexOf("\nauto-messages:")
                && newText.indexOf("\nauto-messages:") < newText.indexOf("\nupdate-checker:"), "at its place from the default");
        assertTrue(newText.indexOf("entries-per-page:") > newText.indexOf("retention-days:"));

        assertEquals(oldText, Files.readString(configFile().resolveSibling("config.yml.bak")));
        assertEquals(9, plugin.getConfigManager().getChatCooldownSeconds(), "the plugin reads the updated file");
        assertFalse(plugin.getConfigManager().getAutoMessages().isEmpty());
    }

    @Test
    void chatGamesFromOlderVersionsGetTheCurrentParticleName() throws Exception {
        File file = new File(plugin.getDataFolder(), "chatgames.yml");
        YamlConfiguration old = YamlConfiguration.loadConfiguration(file);
        old.set("config-version", 2);
        old.set("chat-games.default-reward.particle.name", "TOTEM");
        old.set("default-games.math_solver.reward.particle.name", "TOTEM");
        old.set("default-games.trivia_quiz.reward.particle.name", "HEART");
        old.save(file);

        plugin.reload();

        YamlConfiguration now = YamlConfiguration.loadConfiguration(file);
        assertEquals(3, now.getInt("config-version"));
        assertEquals("TOTEM_OF_UNDYING", now.getString("chat-games.default-reward.particle.name"));
        assertEquals("TOTEM_OF_UNDYING", now.getString("default-games.math_solver.reward.particle.name"));
        assertEquals("HEART", now.getString("default-games.trivia_quiz.reward.particle.name"), "an admin's own choice stays");
    }

    @Test
    void upToDateFilesAreNeverRewritten() throws Exception {
        Path folder = plugin.getDataFolder().toPath();
        Map<Path, String> before = new HashMap<>();
        try (var files = Files.walk(folder)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")).toList()) {
                before.put(file, Files.readString(file));
            }
        }
        assertTrue(before.size() >= 6, before.keySet().toString());

        plugin.reload();

        for (var entry : before.entrySet()) {
            assertEquals(entry.getValue(), Files.readString(entry.getKey()), entry.getKey().toString());
        }
        try (var files = Files.walk(folder)) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".bak")), "no backup without a change");
        }
    }
}

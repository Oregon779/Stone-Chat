package dev.stonechat.plugin.config;

import dev.stonechat.plugin.PluginTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
}

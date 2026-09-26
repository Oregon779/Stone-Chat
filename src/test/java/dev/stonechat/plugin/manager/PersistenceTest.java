package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest extends PluginTestBase {

    private File dataFile(String name) {
        return new File(plugin.getDataFolder(), name);
    }

    /** Stopping the plugin flushes every pending background write. */
    private void stopPlugin() {
        server.getPluginManager().disablePlugin(plugin);
    }

    @Test
    void ignoreListSurvivesARestart() {
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        assertTrue(plugin.getIgnoreManager().toggleIgnore(owner, target));
        stopPlugin();

        IgnoreManager reloaded = new IgnoreManager(plugin);
        assertTrue(reloaded.isIgnoring(owner, target));
    }

    @Test
    void manyRapidChangesEndInTheCorrectFinalState() {
        UUID owner = UUID.randomUUID();
        List<UUID> targets = java.util.stream.Stream.generate(UUID::randomUUID).limit(50).toList();
        for (int round = 0; round < 10; round++) {
            for (UUID target : targets) plugin.getIgnoreManager().toggleIgnore(owner, target);
        }
        plugin.getIgnoreManager().toggleIgnore(owner, targets.get(0));
        stopPlugin();

        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(dataFile("ignorelist.yml"));
        assertEquals(List.of(targets.get(0).toString()), onDisk.getStringList(owner.toString()));
        assertFalse(dataFile("ignorelist.yml.tmp").exists(), "no temp file left behind");
    }

    @Test
    void corruptIgnoreListIsMovedAsideNotOverwritten() throws Exception {
        String garbage = "this: is: not: [valid yaml\n";
        Files.writeString(dataFile("ignorelist.yml").toPath(), garbage);

        IgnoreManager manager = new IgnoreManager(plugin);
        UUID owner = UUID.randomUUID();
        manager.toggleIgnore(owner, UUID.randomUUID());
        stopPlugin();

        try (var files = Files.list(plugin.getDataFolder().toPath())) {
            List<Path> corrupt = files.filter(p -> p.getFileName().toString().startsWith("ignorelist.yml.corrupt-")).toList();
            assertEquals(1, corrupt.size(), "the broken file is kept for manual recovery");
            assertEquals(garbage, Files.readString(corrupt.get(0)));
        }
        assertEquals(1, YamlConfiguration.loadConfiguration(dataFile("ignorelist.yml")).getStringList(owner.toString()).size());
    }

    @Test
    void chatColorsSurviveARestartAndClearingRemovesThem() {
        UUID keep = UUID.randomUUID();
        UUID clear = UUID.randomUUID();
        plugin.getPlayerColorManager().setColor(keep, "&c");
        plugin.getPlayerColorManager().setColor(clear, "&a");
        plugin.getPlayerColorManager().clearColor(clear);
        stopPlugin();

        PlayerColorManager reloaded = new PlayerColorManager(plugin);
        assertEquals("&c", reloaded.getColorCode(keep));
        assertNull(reloaded.getColorCode(clear));
    }
}

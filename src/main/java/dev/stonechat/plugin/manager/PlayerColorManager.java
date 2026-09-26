package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerColorManager {

    private final StoneChat plugin;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();
    private File file;

    public PlayerColorManager(StoneChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "playercolors.yml");

        cache.clear();
        YamlConfiguration data = DataFiles.loadYaml(plugin.getLogger(), file);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String colorCode = data.getString(key);
                if (colorCode != null && !colorCode.isBlank()) {
                    cache.put(uuid, colorCode);
                }
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    public String getColorCode(UUID uuid) {
        return cache.get(uuid);
    }

    public void setColor(UUID uuid, String colorCode) {
        cache.put(uuid, colorCode);
        save();
    }

    public void clearColor(UUID uuid) {
        cache.remove(uuid);
        save();
    }

    private void save() {
        plugin.getDataFiles().writeLater(file.toPath(), () -> {
            YamlConfiguration data = new YamlConfiguration();
            for (var entry : cache.entrySet()) {
                data.set(entry.getKey().toString(), entry.getValue());
            }
            return data.saveToString();
        });
    }
}

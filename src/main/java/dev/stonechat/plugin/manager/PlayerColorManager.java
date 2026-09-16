package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create playercolors.yml", e);
            }
        }

        cache.clear();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
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
        save(uuid, colorCode);
    }

    public void clearColor(UUID uuid) {
        cache.remove(uuid);
        save(uuid, null);
    }

    private void save(UUID uuid, String colorCode) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set(uuid.toString(), colorCode);
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save playercolors.yml", e);
        }
    }
}

package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SoundManager {

    private final StoneChat plugin;
    private final Map<String, Sound> resolvedSoundCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> invalidSoundWarned = new ConcurrentHashMap<>();

    public SoundManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        resolvedSoundCache.clear();
        invalidSoundWarned.clear();
    }

    public void play(Player player, String configPath) {
        play(player, plugin.getConfigManager().raw(), "main", configPath);
    }

    public void playToAll(String configPath) {
        playToAll(plugin.getConfigManager().raw(), "main", configPath);
    }

    public void play(Player player, YamlConfiguration source, String sourceTag, String configPath) {
        if (player == null || !player.isOnline()) return;
        if (!source.getBoolean(configPath + ".enabled", false)) return;

        Sound sound = resolveSound(source, sourceTag, configPath);
        if (sound == null) return;

        float volume = (float) source.getDouble(configPath + ".volume", 1.0);
        float pitch = (float) source.getDouble(configPath + ".pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playToAll(YamlConfiguration source, String sourceTag, String configPath) {
        if (!source.getBoolean(configPath + ".enabled", false)) return;

        Sound sound = resolveSound(source, sourceTag, configPath);
        if (sound == null) return;

        float volume = (float) source.getDouble(configPath + ".volume", 1.0);
        float pitch = (float) source.getDouble(configPath + ".pitch", 1.0);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.playSound(online.getLocation(), sound, volume, pitch);
        }
    }

    private Sound resolveSound(YamlConfiguration source, String sourceTag, String configPath) {
        String cacheKey = sourceTag + ':' + configPath;
        Sound cached = resolvedSoundCache.get(cacheKey);
        if (cached != null) return cached;

        String soundName = source.getString(configPath + ".sound-name", "");
        if (soundName == null || soundName.isBlank()) return null;

        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            resolvedSoundCache.put(cacheKey, sound);
            return sound;
        } catch (IllegalArgumentException e) {

            if (invalidSoundWarned.putIfAbsent(cacheKey, Boolean.TRUE) == null) {
                plugin.getLogger().log(Level.WARNING, "Invalid sound '" + soundName + "' configured at '" + configPath + ".sound-name' - no sound will be played there.");
            }
            return null;
        }
    }
}

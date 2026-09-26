package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SoundManager {

    /** A sound block from the config, resolved once. {@code sound == null} means "don't play anything". */
    private record SoundSetting(Sound sound, float volume, float pitch) {
    }

    private static final SoundSetting SILENT = new SoundSetting(null, 0f, 0f);

    private final StoneChat plugin;
    private final Map<String, SoundSetting> settings = new ConcurrentHashMap<>();

    public SoundManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        settings.clear();
    }

    public void play(Player player, String configPath) {
        play(player, plugin.getConfigManager().raw(), "main", configPath);
    }

    public void playToAll(String configPath) {
        playToAll(plugin.getConfigManager().raw(), "main", configPath);
    }

    public void play(Player player, YamlConfiguration source, String sourceTag, String configPath) {
        if (player == null || !player.isOnline()) return;
        SoundSetting setting = resolve(source, sourceTag, configPath);
        if (setting.sound() == null) return;
        player.playSound(player.getLocation(), setting.sound(), setting.volume(), setting.pitch());
    }

    public void playToAll(YamlConfiguration source, String sourceTag, String configPath) {
        SoundSetting setting = resolve(source, sourceTag, configPath);
        if (setting.sound() == null) return;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.playSound(online.getLocation(), setting.sound(), setting.volume(), setting.pitch());
        }
    }

    private SoundSetting resolve(YamlConfiguration source, String sourceTag, String configPath) {
        return settings.computeIfAbsent(sourceTag + ':' + configPath, key -> read(source, configPath));
    }

    private SoundSetting read(YamlConfiguration source, String configPath) {
        if (!source.getBoolean(configPath + ".enabled", false)) return SILENT;

        String soundName = source.getString(configPath + ".sound-name", "");
        if (soundName == null || soundName.isBlank()) return SILENT;

        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            return new SoundSetting(sound,
                    (float) source.getDouble(configPath + ".volume", 1.0),
                    (float) source.getDouble(configPath + ".pitch", 1.0));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid sound '" + soundName + "' configured at '" + configPath
                    + ".sound-name' - no sound will be played there.");
            return SILENT;
        }
    }
}

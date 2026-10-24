package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/** The server-wide chat mute. Saved in chatmute.yml, so a muted chat stays muted after a restart. */
public class MuteManager {

    private final StoneChat plugin;
    private final File file;
    private volatile boolean muted;

    public MuteManager(StoneChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chatmute.yml");
        this.muted = DataFiles.loadYaml(plugin.getLogger(), file).getBoolean("muted", false);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        save();
    }

    public boolean toggle() {
        setMuted(!muted);
        return muted;
    }

    public boolean canBypass(CommandSender sender, String bypassPermission) {
        return sender.hasPermission(bypassPermission);
    }

    private void save() {
        plugin.getDataFiles().writeLater(file.toPath(), () -> {
            YamlConfiguration data = new YamlConfiguration();
            data.set("muted", muted);
            return data.saveToString();
        });
    }
}

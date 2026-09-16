package dev.stonechat.plugin.manager;

import org.bukkit.command.CommandSender;

public class MuteManager {

    private volatile boolean muted = false;

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean toggle() {
        muted = !muted;
        return muted;
    }

    public boolean canBypass(CommandSender sender, String bypassPermission) {
        return sender.hasPermission(bypassPermission);
    }
}

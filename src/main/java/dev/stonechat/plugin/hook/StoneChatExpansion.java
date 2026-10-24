package dev.stonechat.plugin.hook;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatColorGuiManager;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Stone Chat's own placeholders for other plugins (scoreboards, tab lists, holograms), e.g.
 * %stonechat_chatcolor% or %stonechat_chat_muted%. Only loaded when PlaceholderAPI is installed.
 */
public class StoneChatExpansion extends PlaceholderExpansion {

    private final StoneChat plugin;

    public StoneChatExpansion(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "stonechat";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /** Stone Chat registers this itself, so it must survive /papi reload. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        switch (params.toLowerCase()) {
            case "chat_muted":
                return bool(plugin.getMuteManager().isMuted());
            case "chatgame_running":
                return bool(plugin.getChatGameManager().isRunning());
            default:
                break;
        }
        if (player == null) return null;

        UUID uuid = player.getUniqueId();
        return switch (params.toLowerCase()) {
            case "chatcolor" -> {
                String code = plugin.getPlayerColorManager().getColorCode(uuid);
                yield code == null ? "" : code;
            }
            case "chatcolor_name" -> {
                ChatColorGuiManager.ColorOption option = plugin.getChatColorGuiManager()
                        .findOptionByColorCode(plugin.getPlayerColorManager().getColorCode(uuid));
                yield option == null ? "" : option.displayName();
            }
            case "pings_enabled" -> bool(!plugin.getPingManager().isIgnoringPings(uuid));
            case "ignored_count" -> String.valueOf(plugin.getIgnoreManager().getIgnored(uuid).size());
            case "socialspy" -> bool(plugin.getSocialSpyManager().isSpying(uuid));
            default -> null;
        };
    }

    /** PlaceholderAPI's own yes/no texts, so they match every other placeholder on the server. */
    private static String bool(boolean value) {
        // Touch PlaceholderAPI's plugin class only while it runs; loading it elsewhere fails in its initializer.
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return String.valueOf(value);
        }
        return value ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}

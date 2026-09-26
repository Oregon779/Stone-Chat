package dev.stonechat.plugin.util;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class PlaceholderUtil {

    private static Boolean placeholderApiPresent = null;

    private PlaceholderUtil() {
    }

    private static boolean isPlaceholderApiPresent() {
        if (placeholderApiPresent == null) {
            placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderApiPresent;
    }

    public static String apply(Player player, String text, boolean allowPlaceholderApi) {
        String result = text;

        if (result.contains("%player_name%")) {
            result = result.replace("%player_name%", player.getName());
        }
        if (result.contains("%player_displayname%")) {
            String displayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
            result = result.replace("%player_displayname%", displayName);
        }
        if (result.contains("%player_uuid%")) {
            result = result.replace("%player_uuid%", player.getUniqueId().toString());
        }
        if (result.contains("%player_health%")) {
            result = result.replace("%player_health%", String.valueOf(Math.round(player.getHealth())));
        }
        if (result.contains("%player_max_health%")) {

            result = result.replace("%player_max_health%", String.valueOf(Math.round(player.getMaxHealth())));
        }
        if (result.contains("%player_level%")) {
            result = result.replace("%player_level%", String.valueOf(player.getLevel()));
        }
        if (result.contains("%player_ping%")) {
            result = result.replace("%player_ping%", String.valueOf(player.getPing()));
        }
        if (result.contains("%player_gamemode%")) {
            result = result.replace("%player_gamemode%", player.getGameMode().name());
        }
        if (result.contains("%world%")) {
            result = result.replace("%world%", player.getWorld().getName());
        }
        if (result.contains("%world_players%")) {
            result = result.replace("%world_players%", String.valueOf(player.getWorld().getPlayerCount()));
        }
        if (result.contains("%online%")) {
            result = result.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        if (result.contains("%max_players%")) {
            result = result.replace("%max_players%", String.valueOf(Bukkit.getServer().getMaxPlayers()));
        }
        if (result.contains("%luckperms_rank%")) {
            String rank = LuckPermsUtil.getPrimaryGroup(player);
            result = result.replace("%luckperms_rank%", rank != null ? rank : "");
        }
        if (result.contains("%luckperms_prefix%") || result.contains("%prefix%")) {
            String prefix = LuckPermsUtil.getPrefix(player);
            result = result.replace("%luckperms_prefix%", prefix != null ? prefix : "");

            result = result.replace("%prefix%", prefix != null ? prefix : "");
        }
        if (result.contains("%luckperms_suffix%") || result.contains("%suffix%")) {
            String suffix = LuckPermsUtil.getSuffix(player);
            result = result.replace("%luckperms_suffix%", suffix != null ? suffix : "");
            result = result.replace("%suffix%", suffix != null ? suffix : "");
        }

        if (allowPlaceholderApi && isPlaceholderApiPresent()) {
            result = setPlaceholders(player, result);
        }
        return result;
    }

    private static volatile boolean placeholderApiInitialized = false;
    private static Method placeholderApiSetPlaceholders;

    private static String setPlaceholders(Player player, String text) {
        if (!placeholderApiInitialized) {
            synchronized (PlaceholderUtil.class) {
                if (!placeholderApiInitialized) {
                    try {
                        Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                        placeholderApiSetPlaceholders = clazz.getMethod("setPlaceholders", Player.class, String.class);
                    } catch (Exception e) {
                        placeholderApiSetPlaceholders = null;
                    }
                    placeholderApiInitialized = true;
                }
            }
        }
        if (placeholderApiSetPlaceholders == null) return text;
        try {
            Object result = placeholderApiSetPlaceholders.invoke(null, player, text);
            return (String) result;
        } catch (Exception e) {
            return text;
        }
    }
}

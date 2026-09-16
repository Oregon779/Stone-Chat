package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final StoneChat plugin;
    private final Map<UUID, Long> chatCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> commandCooldowns = new ConcurrentHashMap<>();

    public CooldownManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    private boolean bypasses(Player player) {
        return player.hasPermission(plugin.getConfigManager().getCooldownBypassPermission());
    }

    public int tryChat(Player player) {
        if (!plugin.getConfigManager().isCooldownEnabled() || bypasses(player)) return 0;

        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfigManager().getChatCooldownSeconds() * 1000L;
        Long last = chatCooldowns.get(player.getUniqueId());

        if (last != null && now - last < cooldownMs) {
            return (int) Math.ceil((cooldownMs - (now - last)) / 1000.0);
        }

        chatCooldowns.put(player.getUniqueId(), now);
        return 0;
    }

    public int tryCommand(Player player, String commandLabel) {
        if (!plugin.getConfigManager().isCooldownEnabled() || bypasses(player)) return 0;

        var configured = plugin.getConfigManager().getCooldownCommands();
        boolean affected = configured.isEmpty() || configured.stream().anyMatch(c -> c.equalsIgnoreCase(commandLabel));
        if (!affected) return 0;

        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfigManager().getCommandCooldownSeconds() * 1000L;
        Long last = commandCooldowns.get(player.getUniqueId());

        if (last != null && now - last < cooldownMs) {
            return (int) Math.ceil((cooldownMs - (now - last)) / 1000.0);
        }

        commandCooldowns.put(player.getUniqueId(), now);
        return 0;
    }

    public void clear(UUID uuid) {
        chatCooldowns.remove(uuid);
        commandCooldowns.remove(uuid);
    }
}

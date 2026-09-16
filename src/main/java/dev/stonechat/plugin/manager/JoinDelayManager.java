package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinDelayManager {

    private final StoneChat plugin;
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public JoinDelayManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void markJoined(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clear(UUID uuid) {
        joinTimes.remove(uuid);
    }

    public int remainingSeconds(Player player) {
        if (!plugin.getConfigManager().isJoinDelayEnabled()) return 0;
        if (player.hasPermission(plugin.getConfigManager().getJoinDelayBypassPermission())) return 0;

        Long joinTime = joinTimes.get(player.getUniqueId());
        if (joinTime == null) return 0;

        long delayMs = plugin.getConfigManager().getJoinDelaySeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - joinTime;

        if (elapsed >= delayMs) return 0;
        return (int) Math.ceil((delayMs - elapsed) / 1000.0);
    }
}

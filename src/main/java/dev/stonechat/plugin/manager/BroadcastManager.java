package dev.stonechat.plugin.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;

public class BroadcastManager {

    public enum Type {
        CHAT, ACTIONBAR, TITLE, BOSSBAR
    }

    private final StoneChat plugin;

    public BroadcastManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void broadcast(Type type, String rawMessage) {
        broadcast(type, rawMessage, null);
    }

    public void broadcast(Type type, String rawMessage, Integer durationSeconds) {
        render(type, rawMessage, durationSeconds, List.copyOf(Bukkit.getOnlinePlayers()), true);
    }

    /** Like {@link #broadcast(Type, String)}, but never adds the "[Broadcast]" prefix. */
    public void broadcastWithoutPrefix(Type type, String rawMessage) {
        render(type, rawMessage, null, List.copyOf(Bukkit.getOnlinePlayers()), false);
    }

    public void preview(Player player, Type type, String rawMessage, Integer durationSeconds) {
        render(type, rawMessage, durationSeconds, List.of(player), true);
    }

    private void render(Type type, String rawMessage, Integer durationSeconds, List<Player> targets, boolean allowPrefix) {
        String withPrefix = (allowPrefix && type == Type.CHAT && plugin.getConfigManager().isBroadcastUsePrefix())
                ? plugin.getLanguageManager().getRaw("broadcast.prefix") + rawMessage
                : rawMessage;
        Component message = ColorUtil.parse(withPrefix);

        switch (type) {
            case CHAT -> {
                for (Player player : targets) {
                    player.sendMessage(message);
                }
            }
            case ACTIONBAR -> showActionbar(message, durationSeconds, targets);
            case TITLE -> showTitle(message, durationSeconds, targets);
            case BOSSBAR -> showBossbar(message, durationSeconds, targets);
        }
    }

    private void showActionbar(Component message, Integer durationSeconds, List<Player> targets) {

        int seconds = durationSeconds != null ? Math.max(1, durationSeconds) : 3;

        new BukkitRunnable() {
            int elapsedTicks = 0;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
                if (elapsedTicks >= totalTicks) {
                    cancel();
                    return;
                }
                for (Player player : targets) {
                    if (player.isOnline()) player.sendActionBar(message);
                }
                elapsedTicks += 40;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void showTitle(Component message, Integer durationSeconds, List<Player> targets) {
        int stayTicks = durationSeconds != null ? Math.max(1, durationSeconds) * 20 : plugin.getConfigManager().getTitleStay();
        Title.Times times = Title.Times.times(
                Duration.ofMillis(plugin.getConfigManager().getTitleFadeIn() * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(plugin.getConfigManager().getTitleFadeOut() * 50L)
        );
        Title title = Title.title(message, Component.empty(), times);
        for (Player player : targets) {
            player.showTitle(title);
        }
    }

    private void showBossbar(Component message, Integer durationSeconds, List<Player> targets) {
        BossBar.Color color;
        BossBar.Overlay overlay;
        try {
            color = BossBar.Color.valueOf(plugin.getConfigManager().getBossbarColor().toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.BLUE;
        }
        try {
            overlay = BossBar.Overlay.valueOf(plugin.getConfigManager().getBossbarStyle().toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        BossBar bossBar = BossBar.bossBar(message, 1.0f, color, overlay);

        for (Player player : targets) {
            player.showBossBar(bossBar);
        }

        int seconds = durationSeconds != null ? Math.max(1, durationSeconds) : plugin.getConfigManager().getBossbarDurationSeconds();
        int totalTicks = seconds * 20;

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed += 5;
                float remaining = Math.max(0f, 1f - ((float) elapsed / totalTicks));
                bossBar.progress(remaining);

                if (elapsed >= totalTicks) {
                    for (Player player : targets) {
                        player.hideBossBar(bossBar);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}

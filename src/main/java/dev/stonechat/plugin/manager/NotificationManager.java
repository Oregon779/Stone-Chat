package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.model.MessageDisplayType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class NotificationManager {

    private static final long BAR_UPDATE_TICKS = 5L;

    private final StoneChat plugin;
    // One notification bossbar per player, driven by a single shared task. Main thread only.
    private final Map<UUID, TimedBar> activeBars = new HashMap<>();
    private BukkitTask barTask;

    public NotificationManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    private static final class TimedBar {
        final BossBar bar;
        int elapsedTicks;
        int totalTicks;

        TimedBar(BossBar bar, int totalTicks) {
            this.bar = bar;
            this.totalTicks = totalTicks;
        }
    }

    public void dispatch(Player target, MessageDisplayType type, String basePath, Map<String, String> placeholders) {
        LanguageManager lm = plugin.getLanguageManager();

        switch (type) {
            case CHAT -> target.sendMessage(lm.getPrefixed(basePath + ".chat", placeholders));
            case ACTIONBAR -> target.sendActionBar(lm.get(basePath + ".actionbar", placeholders));
            case TITLE -> showTitle(target,
                    lm.get(basePath + ".title", placeholders),
                    lm.get(basePath + ".subtitle", placeholders));
            case BOSSBAR -> showBossbar(target, lm.get(basePath + ".bossbar", placeholders));
        }
    }

    public void broadcast(MessageDisplayType type, String basePath, Map<String, String> placeholders) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            dispatch(online, type, basePath, placeholders);
        }
    }

    /** Removes every notification bossbar still on screen, e.g. when the plugin is disabled. */
    public void hideAllBossbars() {
        for (Map.Entry<UUID, TimedBar> entry : activeBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) player.hideBossBar(entry.getValue().bar);
        }
        activeBars.clear();
        if (barTask != null) {
            barTask.cancel();
            barTask = null;
        }
    }

    private void showTitle(Player player, Component title, Component subtitle) {
        int fadeIn = plugin.getConfigManager().getTitleFadeIn();
        int stay = plugin.getConfigManager().getTitleStay();
        int fadeOut = plugin.getConfigManager().getTitleFadeOut();
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );
        player.showTitle(Title.title(title, subtitle, times));
    }

    private void showBossbar(Player player, Component message) {
        if (!Bukkit.isPrimaryThread()) {
            // Notifications can come from the async chat thread; bossbar state is kept on the main thread.
            Bukkit.getScheduler().runTask(plugin, () -> showBossbar(player, message));
            return;
        }
        if (!player.isOnline()) return;

        int totalTicks = Math.max(1, plugin.getConfigManager().getBossbarDurationSeconds() * 20);
        TimedBar existing = activeBars.get(player.getUniqueId());
        if (existing != null) {
            existing.bar.name(message);
            existing.bar.progress(1f);
            existing.elapsedTicks = 0;
            existing.totalTicks = totalTicks;
            return;
        }

        BossBar bossBar = BossBar.bossBar(message, 1.0f, resolveColor(), resolveOverlay());
        player.showBossBar(bossBar);
        activeBars.put(player.getUniqueId(), new TimedBar(bossBar, totalTicks));
        if (barTask == null) {
            barTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBars, BAR_UPDATE_TICKS, BAR_UPDATE_TICKS);
        }
    }

    private void tickBars() {
        Iterator<Map.Entry<UUID, TimedBar>> iterator = activeBars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TimedBar> entry = iterator.next();
            TimedBar timed = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            timed.elapsedTicks += (int) BAR_UPDATE_TICKS;
            if (timed.elapsedTicks >= timed.totalTicks) {
                player.hideBossBar(timed.bar);
                iterator.remove();
                continue;
            }
            timed.bar.progress(Math.max(0f, 1f - ((float) timed.elapsedTicks / timed.totalTicks)));
        }
        if (activeBars.isEmpty() && barTask != null) {
            barTask.cancel();
            barTask = null;
        }
    }

    private BossBar.Color resolveColor() {
        try {
            return BossBar.Color.valueOf(plugin.getConfigManager().getBossbarColor().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay resolveOverlay() {
        try {
            return BossBar.Overlay.valueOf(plugin.getConfigManager().getBossbarStyle().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}

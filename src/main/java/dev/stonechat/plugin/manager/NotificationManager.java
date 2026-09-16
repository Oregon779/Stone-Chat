package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.model.MessageDisplayType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Map;

public class NotificationManager {

    private final StoneChat plugin;

    public NotificationManager(StoneChat plugin) {
        this.plugin = plugin;
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
        player.showBossBar(bossBar);

        int totalTicks = Math.max(1, plugin.getConfigManager().getBossbarDurationSeconds() * 20);

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                elapsed += 5;
                float remaining = Math.max(0f, 1f - ((float) elapsed / totalTicks));
                bossBar.progress(remaining);

                if (elapsed >= totalTicks) {
                    player.hideBossBar(bossBar);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}

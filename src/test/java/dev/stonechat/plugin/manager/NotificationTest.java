package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest extends PluginTestBase {

    private PlayerMock steve;

    @BeforeEach
    void bossbarCooldownNotifications() {
        plugin.getConfigManager().set("cooldown.enabled", true);
        plugin.getConfigManager().set("cooldown.notification-type", "BOSSBAR");
        steve = server.addPlayer("Steve");
    }

    @Test
    void repeatedBossbarNotificationsReuseOneBarInsteadOfStacking() throws Exception {
        chat(steve, "first");
        for (int i = 0; i < 5; i++) chat(steve, "spam " + i);
        server.getScheduler().performOneTick();

        assertEquals(1, steve.getBossBars().size());
    }

    @Test
    void barDisappearsAfterItsDuration() throws Exception {
        chat(steve, "first");
        chat(steve, "too fast");
        server.getScheduler().performOneTick();
        assertEquals(1, steve.getBossBars().size());

        server.getScheduler().performTicks(plugin.getConfigManager().getBossbarDurationSeconds() * 20L + 10);

        assertTrue(steve.getBossBars().isEmpty());
    }

    @Test
    void disablingThePluginRemovesVisibleBars() throws Exception {
        chat(steve, "first");
        chat(steve, "too fast");
        server.getScheduler().performOneTick();

        server.getPluginManager().disablePlugin(plugin);

        assertTrue(steve.getBossBars().isEmpty());
    }
}

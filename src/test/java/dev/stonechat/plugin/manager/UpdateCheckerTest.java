package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerTest extends PluginTestBase {

    private Object scheduledTask() throws Exception {
        Field field = UpdateChecker.class.getDeclaredField("task");
        field.setAccessible(true);
        return field.get(plugin.getUpdateChecker());
    }

    @AfterEach
    void noNetworkAfterwards() {
        plugin.getConfigManager().set("update-checker.enabled", false);
        plugin.getUpdateChecker().start();
    }

    @Test
    void reloadingWithUnchangedSettingsDoesNotRescheduleTheHttpCheck() throws Exception {
        plugin.getConfigManager().set("update-checker.enabled", true);
        plugin.reload();
        Object task = scheduledTask();
        assertNotNull(task);

        for (int i = 0; i < 5; i++) plugin.reload();
        assertSame(task, scheduledTask(), "every editor click reloads the plugin, that must not query Modrinth again");

        plugin.getConfigManager().set("update-checker.check-interval-minutes", 120);
        plugin.reload();
        assertNotSame(task, scheduledTask(), "a changed interval is picked up");
    }
}

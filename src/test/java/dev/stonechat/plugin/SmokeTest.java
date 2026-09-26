package dev.stonechat.plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class SmokeTest extends PluginTestBase {

    @Test
    void pluginEnablesAndRendersChat() throws Exception {
        assertTrue(plugin.isEnabled());
        PlayerMock steve = server.addPlayer("Steve");
        PlayerMock alex = server.addPlayer("Alex");

        AsyncChatEvent event = chat(steve, "hello world");

        assertFalse(event.isCancelled());
        assertEquals("Steve » hello world", rendered(event, alex));
    }
}

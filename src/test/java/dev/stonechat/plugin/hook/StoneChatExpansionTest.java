package dev.stonechat.plugin.hook;

import dev.stonechat.plugin.PluginTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/** The %stonechat_...% placeholders other plugins can use. */
class StoneChatExpansionTest extends PluginTestBase {

    private StoneChatExpansion expansion;
    private PlayerMock steve;

    @BeforeEach
    void setUp() {
        expansion = new StoneChatExpansion(plugin);
        steve = server.addPlayer("Steve");
    }

    private String value(String params) {
        return expansion.onRequest(steve, params);
    }

    @Test
    void identifiesAsStoneChatAndSurvivesPapiReloads() {
        assertEquals("stonechat", expansion.getIdentifier());
        assertEquals(plugin.getPluginMeta().getVersion(), expansion.getVersion());
        assertTrue(expansion.persist());
    }

    @Test
    void serverWidePlaceholdersWorkWithoutAPlayer() {
        assertEquals("false", expansion.onRequest(null, "chat_muted"));
        plugin.getMuteManager().setMuted(true);
        assertEquals("true", expansion.onRequest(null, "chat_muted"));
        assertEquals("false", expansion.onRequest(null, "chatgame_running"));
        assertNull(expansion.onRequest(null, "chatcolor"), "player placeholders need a player");
    }

    @Test
    void chatColorAsCodeAndAsName() {
        assertEquals("", value("chatcolor"));
        assertEquals("", value("chatcolor_name"));

        plugin.getPlayerColorManager().setColor(steve.getUniqueId(), "&c");

        assertEquals("&c", value("chatcolor"));
        assertEquals("&cRed", value("chatcolor_name"));
    }

    @Test
    void personalSettings() {
        assertEquals("true", value("pings_enabled"));
        plugin.getPingManager().toggleIgnorePings(steve);
        assertEquals("false", value("pings_enabled"));

        PlayerMock alex = server.addPlayer("Alex");
        assertEquals("0", value("ignored_count"));
        plugin.getIgnoreManager().toggleIgnore(steve.getUniqueId(), alex.getUniqueId());
        assertEquals("1", value("ignored_count"));

        assertEquals("false", value("socialspy"));
        plugin.getSocialSpyManager().toggle(steve);
        assertEquals("true", value("socialspy"));
    }

    @Test
    void unknownPlaceholdersAreLeftForPlaceholderApi() {
        assertNull(value("does_not_exist"));
    }
}

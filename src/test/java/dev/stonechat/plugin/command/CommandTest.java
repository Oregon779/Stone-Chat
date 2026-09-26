package dev.stonechat.plugin.command;

import dev.stonechat.plugin.PluginTestBase;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest extends PluginTestBase {

    private PlayerMock admin;
    private PlayerMock steve;
    private PlayerMock alex;

    @BeforeEach
    void players() {
        admin = server.addPlayer("Admin");
        admin.setOp(true);
        steve = server.addPlayer("Steve");
        alex = server.addPlayer("Alex");
        inbox(admin);
        inbox(steve);
        inbox(alex);
    }

    private boolean anyContains(List<String> messages, String text) {
        return messages.stream().anyMatch(m -> m.contains(text));
    }

    private PlayerCommandPreprocessEvent preprocess(PlayerMock player, String commandLine) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, commandLine);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void broadcastNeedsPermissionAndSurvivesAbsurdDurations() {
        steve.performCommand("broadcast chat hello");
        assertTrue(anyContains(inbox(steve), "do not have permission"));

        // 999999999 s * 20 ticks used to overflow int, so the actionbar was cancelled before it ever showed.
        admin.performCommand("broadcast actionbar 999999999 Hi");
        server.getScheduler().performOneTick();
        assertEquals("Hi", PLAIN.serialize(admin.nextActionBar()));
        admin.performCommand("broadcast chat");
        assertTrue(anyContains(inbox(admin), "Usage: /broadcast"));
    }

    @Test
    void chatClearIsCappedAtWhatTheClientKeeps() {
        plugin.getConfigManager().set("chat-clear.lines", 100000);
        admin.performCommand("chatclear");

        List<String> got = inbox(alex);
        assertEquals(100, got.stream().filter(String::isEmpty).count());
        assertTrue(anyContains(got, "cleared by Admin"));
    }

    @Test
    void privateMessagesHandleEveryBadInput() {
        steve.performCommand("msg");
        assertTrue(anyContains(inbox(steve), "Usage: /msg"));
        steve.performCommand("msg Nobody hi");
        assertTrue(anyContains(inbox(steve), "not online"));
        steve.performCommand("msg Steve hi");
        assertTrue(anyContains(inbox(steve), "cannot message yourself"));

        steve.performCommand("msg Alex <red>hi <click:run_command:'/op Steve'>there");
        List<String> alexGot = inbox(alex).stream().map(m -> m.replace("\u200B", "")).toList();
        assertTrue(anyContains(alexGot, "Steve"), alexGot.toString());
        assertTrue(anyContains(alexGot, "<red>hi <click:run_command:'/op Steve'>there"), "shown as text, not executed");

        alex.performCommand("r thanks");
        assertTrue(anyContains(inbox(steve), "thanks"));
    }

    @Test
    void ignoredPlayersCannotMessageYou() {
        alex.performCommand("ignore Steve");
        assertTrue(anyContains(inbox(alex), "now ignoring"));

        steve.performCommand("msg Alex hello?");
        assertTrue(anyContains(inbox(steve), "cannot message Alex"));
        assertTrue(inbox(alex).isEmpty());

        alex.performCommand("ignore list");
        assertTrue(anyContains(inbox(alex), "Steve"));
    }

    @Test
    void immunePlayersCannotBeIgnored() {
        steve.addAttachment(plugin, "stonechat.ignore.immune", true);
        alex.performCommand("ignore Steve");
        assertTrue(anyContains(inbox(alex), "cannot ignore this player"));
        assertFalse(plugin.getIgnoreManager().isIgnoring(alex.getUniqueId(), steve.getUniqueId()));
    }

    @Test
    void chatLogRejectsBadArgumentsAndOutsiders() {
        steve.performCommand("chatlog Alex");
        assertTrue(inbox(steve).stream().noneMatch(m -> m.contains("Chat log of")), "no permission, no log");

        admin.performCommand("chatlog");
        assertTrue(anyContains(inbox(admin), "Usage: /chatlog"));
        admin.performCommand("chatlog Alex notanumber");
        assertTrue(anyContains(inbox(admin), "Usage: /chatlog"));
        admin.performCommand("chatlog Alex 1 2");
        assertTrue(anyContains(inbox(admin), "Usage: /chatlog"));
        admin.performCommand("chatlog DefinitelyNotAPlayer");
        assertTrue(anyContains(inbox(admin), "doesn't know a player"));
    }

    @Test
    void namespacedCommandsDoNotDodgeTheCooldown() {
        plugin.getConfigManager().set("cooldown.enabled", true);
        plugin.getConfigManager().set("cooldown.cooldown-commands", List.of("msg"));

        assertFalse(preprocess(steve, "/msg Alex one").isCancelled());
        assertTrue(preprocess(steve, "/stonechat:msg Alex two").isCancelled());
    }

    @Test
    void mutedChatBlocksCommandsButCancelledCommandsAreLeftAlone() {
        plugin.getMuteManager().setMuted(true);
        assertTrue(preprocess(steve, "/msg Alex hi").isCancelled());
        assertTrue(preprocess(steve, "/minecraft:tell Alex hi").isCancelled());
        assertFalse(preprocess(steve, "/stonechat settings").isCancelled(), "own settings stay reachable");

        plugin.getMuteManager().setMuted(false);
        plugin.getConfigManager().set("cooldown.enabled", true);
        PlayerCommandPreprocessEvent alreadyCancelled = new PlayerCommandPreprocessEvent(steve, "/msg Alex hi");
        alreadyCancelled.setCancelled(true);
        server.getPluginManager().callEvent(alreadyCancelled);
        assertFalse(preprocess(steve, "/msg Alex hi").isCancelled(), "a command another plugin cancelled must not start the cooldown");
    }

    @Test
    void chatGameCommandValidatesInput() {
        admin.performCommand("chatgame");
        assertTrue(anyContains(inbox(admin), "Usage: /chatgame"));
        admin.performCommand("chatgame start doesnotexist");
        assertTrue(anyContains(inbox(admin), "No chat game found"));
        admin.performCommand("chatgame stop");
        assertTrue(anyContains(inbox(admin), "No chat game is currently running"));
        admin.performCommand("chatgame list");
        assertTrue(anyContains(inbox(admin), "math_solver"));
    }

    @Test
    void reloadIsAdminOnlyAndKeepsWorking() {
        steve.performCommand("stonechat reload");
        assertTrue(anyContains(inbox(steve), "do not have permission"));
        for (int i = 0; i < 5; i++) admin.performCommand("stonechat reload");
        assertTrue(anyContains(inbox(admin), "reloaded"));
        assertEquals(1, server.getPluginManager().getPlugins().length);
    }
}

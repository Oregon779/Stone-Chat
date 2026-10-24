package dev.stonechat.plugin.command;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.manager.ChatLogManager;
import dev.stonechat.plugin.manager.SocialSpyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** /msg gets the same checks as public chat, is written to the chat log and can be watched with /socialspy. */
class PrivateMessageModerationTest extends PluginTestBase {

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

    private static boolean anyContains(List<String> messages, String text) {
        return messages.stream().anyMatch(m -> m.replace("​", "").contains(text));
    }

    /** Chat log reads answer on the main thread after the log thread is done, so keep ticking until they arrive. */
    private List<ChatLogManager.Entry> logOf(PlayerMock player) throws Exception {
        CompletableFuture<ChatLogManager.Page> page = new CompletableFuture<>();
        plugin.getChatLogManager().readPage(player.getUniqueId(), 1, 50, page::complete);
        for (int i = 0; i < 200 && !page.isDone(); i++) {
            Thread.sleep(5);
            server.getScheduler().performOneTick();
        }
        return page.get(1, TimeUnit.SECONDS).entries();
    }

    @Test
    void aForbiddenWordBlocksThePrivateMessageAndIsLogged() throws Exception {
        steve.performCommand("msg Alex you badword1");

        assertTrue(inbox(alex).isEmpty(), "Alex never sees it");
        assertTrue(anyContains(inbox(steve), "contains a forbidden word"));
        ChatLogManager.Entry entry = logOf(steve).get(0);
        assertEquals(ChatLogManager.Status.BLOCKED_WORD_FILTER, entry.status());
        assertEquals("Alex", entry.receiver());
        assertEquals("you badword1", entry.message());
    }

    @Test
    void censorModeDeliversTheCensoredText() {
        plugin.getConfigManager().set("word-filter.action", "censor");
        plugin.reload();

        steve.performCommand("msg Alex you badword1");

        List<String> alexGot = inbox(alex);
        assertTrue(anyContains(alexGot, "you ********"), alexGot.toString());
        assertFalse(anyContains(alexGot, "badword1"));
    }

    @Test
    void linksAndCapsAreBlockedUnlessBypassed() {
        steve.performCommand("msg Alex join example.com now");
        assertTrue(anyContains(inbox(steve), "links in chat is not allowed"));
        steve.performCommand("msg Alex HELLO EVERYONE");
        assertTrue(anyContains(inbox(steve), "excessive capital letters"));
        assertTrue(inbox(alex).isEmpty());

        steve.addAttachment(plugin, "stonechat.links.bypass", true);
        steve.performCommand("msg Alex join example.com now");
        assertTrue(anyContains(inbox(alex), "join example.com now"));
    }

    @Test
    void eachCheckCanBeSwitchedOffForPrivateMessages() {
        plugin.getConfigManager().set("private-messages.filter.word-filter", false);
        plugin.reload();

        steve.performCommand("msg Alex you badword1");

        assertTrue(anyContains(inbox(alex), "you badword1"));
    }

    @Test
    void deliveredMessagesAreLoggedWithTheReceiver() throws Exception {
        steve.performCommand("msg Alex see you later");
        alex.performCommand("r bye");

        ChatLogManager.Entry steveEntry = logOf(steve).get(0);
        assertEquals(ChatLogManager.Status.SENT, steveEntry.status());
        assertEquals("Alex", steveEntry.receiver());
        assertEquals("see you later", steveEntry.message());
        assertEquals("Steve", logOf(alex).get(0).receiver());
    }

    @Test
    void chatLogMarksPrivateMessages() throws Exception {
        steve.performCommand("msg Alex secret plan");
        logOf(steve);

        admin.performCommand("chatlog Steve");
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < 200 && shown.size() < 2; i++) {
            Thread.sleep(5);
            server.getScheduler().performOneTick();
            shown.addAll(inbox(admin));
        }
        assertTrue(anyContains(shown, "[PM to Alex] secret plan"), shown.toString());
    }

    @Test
    void privateMessagesAreNotLoggedWhenSwitchedOff() throws Exception {
        plugin.getConfigManager().set("chat-log.log-private-messages", false);
        plugin.reload();

        steve.performCommand("msg Alex nobody reads this");

        assertTrue(logOf(steve).isEmpty());
    }

    @Test
    void socialSpySeesOtherPeoplesMessagesUntilTurnedOff() {
        admin.performCommand("socialspy");
        assertTrue(anyContains(inbox(admin), "Social spy is on"));

        steve.performCommand("msg Alex meet at spawn");
        assertTrue(anyContains(inbox(admin), "[Spy] Steve -> Alex: meet at spawn"));
        assertFalse(anyContains(inbox(steve), "[Spy]"), "the sender gets no copy");
        assertFalse(anyContains(inbox(alex), "[Spy]"), "neither does the receiver");

        admin.performCommand("socialspy");
        steve.performCommand("msg Alex second");
        assertFalse(anyContains(inbox(admin), "second"));
    }

    @Test
    void socialSpyNeedsThePermissionAndSurvivesARestart() {
        steve.performCommand("socialspy");
        assertTrue(anyContains(inbox(steve), "permission"));

        admin.performCommand("socialspy");
        server.getPluginManager().disablePlugin(plugin);

        assertTrue(new SocialSpyManager(plugin).isSpying(admin));
    }
}

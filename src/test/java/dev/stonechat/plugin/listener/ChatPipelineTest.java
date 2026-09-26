package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.manager.ChatLogManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatPipelineTest extends PluginTestBase {

    private PlayerMock steve;
    private PlayerMock alex;

    @BeforeEach
    void players() {
        steve = server.addPlayer("Steve");
        alex = server.addPlayer("Alex");
        inbox(steve);
        inbox(alex);
    }

    private static boolean hasColor(Component component, TextColor color) {
        if (color.equals(component.color())) return true;
        for (Component child : component.children()) {
            if (hasColor(child, color)) return true;
        }
        return false;
    }

    private List<ChatLogManager.Status> loggedStatuses(PlayerMock player) throws Exception {
        CompletableFuture<ChatLogManager.Page> page = new CompletableFuture<>();
        plugin.getChatLogManager().readPage(player.getUniqueId(), 1, 100, page::complete);
        for (int i = 0; i < 20 && !page.isDone(); i++) {
            Thread.sleep(25);
            server.getScheduler().performOneTick();
        }
        return page.get(1, TimeUnit.SECONDS).entries().stream().map(ChatLogManager.Entry::status).toList();
    }

    @Test
    void normalMessageIsRenderedNotCancelled() throws Exception {
        AsyncChatEvent event = chat(steve, "hello world");

        assertFalse(event.isCancelled());
        assertEquals("Steve » hello world", rendered(event, alex));
        assertEquals("hello world", PLAIN.serialize(event.message()), "other plugins (e.g. Discord bridges) read this");
        assertTrue(hasClickCommand(renderedComponent(event, alex), "/msg Steve "));
        assertTrue(inbox(alex).isEmpty(), "delivery is Paper's job now, the plugin must not broadcast by itself");
    }

    @Test
    void playerTextCannotInjectFormattingOrClicks() throws Exception {
        AsyncChatEvent event = chat(steve, "<red>free <click:run_command:'/op Steve'>op</click> &4now");
        Component line = renderedComponent(event, alex);

        assertFalse(hasClickCommand(line, "/op"));
        assertFalse(hasColor(line, NamedTextColor.RED));
        assertFalse(hasColor(line, NamedTextColor.DARK_RED));
    }

    @Test
    void wordFilterBlocksCensorsAndCatchesUnicodeEvasion() throws Exception {
        assertTrue(chat(steve, "you badword1").isCancelled());
        assertTrue(chat(steve, "you bаdword1").isCancelled(), "Cyrillic a look-alike");
        assertTrue(chat(steve, "you bad​word1").isCancelled(), "zero-width space inside the word");
        assertFalse(chat(steve, "badword10 is fine").isCancelled(), "whole words only");

        plugin.getConfigManager().set("word-filter.action", "censor");
        AsyncChatEvent censored = chat(steve, "you badword1 there");
        assertFalse(censored.isCancelled());
        assertEquals("Steve » you ******** there", rendered(censored, alex));
    }

    @Test
    void linkBlockerRespectsBypassPermission() throws Exception {
        assertTrue(chat(steve, "join www.example.com").isCancelled());
        steve.addAttachment(plugin, "stonechat.links.bypass", true);
        assertFalse(chat(steve, "join www.example.com").isCancelled());
    }

    @Test
    void antiCapsBlocksOrAutocorrects() throws Exception {
        assertTrue(chat(steve, "HELLO EVERYONE").isCancelled());
        assertFalse(chat(steve, "Hi").isCancelled(), "too short to count");

        plugin.getConfigManager().set("anti-caps.mode", "autocorrect");
        AsyncChatEvent corrected = chat(steve, "HELLO EVERYONE");
        assertFalse(corrected.isCancelled());
        assertEquals("Steve » hello everyone", rendered(corrected, alex));
    }

    @Test
    void chatMuteBlocksEveryoneWithoutBypass() throws Exception {
        plugin.getMuteManager().setMuted(true);
        assertTrue(chat(steve, "hi there").isCancelled());
        steve.addAttachment(plugin, "stonechat.mute.bypass", true);
        assertFalse(chat(steve, "hi there").isCancelled());
    }

    @Test
    void cooldownBlocksTheSecondMessage() throws Exception {
        plugin.getConfigManager().set("cooldown.enabled", true);
        assertFalse(chat(steve, "first").isCancelled());
        assertTrue(chat(steve, "second").isCancelled());
        assertFalse(chat(alex, "other players are unaffected").isCancelled());
    }

    @Test
    void joinDelayBlocksChatRightAfterJoining() throws Exception {
        plugin.getConfigManager().set("join-delay.enabled", true);
        PlayerMock newcomer = server.addPlayer("Newcomer");
        assertTrue(chat(newcomer, "hi").isCancelled());
    }

    @Test
    void maxLengthCountsWhatThePlayerTyped() throws Exception {
        plugin.getConfigManager().set("max-message-length.max-length", 10);
        assertFalse(chat(steve, "a<b&c<d&ef").isCancelled(), "exactly 10 typed characters");
        assertTrue(chat(steve, "a<b&c<d&efg").isCancelled());
    }

    @Test
    void repeatedMentionsNotifyOnlyOnce() throws Exception {
        AsyncChatEvent event = chat(steve, "@Alex @Alex @Alex look");
        server.getScheduler().performOneTick();

        assertEquals("Steve » @Alex @Alex @Alex look", rendered(event, alex));
        List<String> alexGot = inbox(alex);
        assertEquals(1, alexGot.stream().filter(m -> m.contains("mentioned you")).count(), alexGot.toString());
    }

    @Test
    void immunePlayersAreNotPinged() throws Exception {
        alex.addAttachment(plugin, "stonechat.ping.immune", true);
        chat(steve, "hey @Alex");
        server.getScheduler().performOneTick();
        assertTrue(inbox(alex).stream().noneMatch(m -> m.contains("mentioned you")));
    }

    @Test
    void permissionGatedChatColorStopsWhenThePermissionIsGone() throws Exception {
        plugin.getPlayerColorManager().setColor(steve.getUniqueId(), "&6");

        AsyncChatEvent withoutPermission = chat(steve, "gold?");
        assertFalse(hasColor(withoutPermission.message(), NamedTextColor.GOLD));

        steve.addAttachment(plugin, "stonechat.color.gold", true);
        AsyncChatEvent withPermission = chat(steve, "gold!");
        assertTrue(hasColor(withPermission.message(), NamedTextColor.GOLD));
    }

    @Test
    void freeChatColorsAlwaysApply() throws Exception {
        plugin.getPlayerColorManager().setColor(steve.getUniqueId(), "&e");
        assertTrue(hasColor(chat(steve, "yellow").message(), NamedTextColor.YELLOW));
    }

    @Test
    void everyAttemptIsLoggedWithItsOutcome() throws Exception {
        chat(steve, "hello");
        chat(steve, "you badword1");
        plugin.getConfigManager().set("word-filter.action", "censor");
        chat(steve, "you badword1");
        chat(steve, "see www.site.com");

        assertEquals(List.of(ChatLogManager.Status.SENT, ChatLogManager.Status.BLOCKED_WORD_FILTER,
                ChatLogManager.Status.CENSORED, ChatLogManager.Status.BLOCKED_LINK), loggedStatuses(steve));
    }
}

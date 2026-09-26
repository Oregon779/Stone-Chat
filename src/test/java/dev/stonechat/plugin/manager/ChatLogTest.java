package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatLogTest extends PluginTestBase {

    /** Reads go through the log's own thread and answer on the main thread, so keep ticking until they arrive. */
    private ChatLogManager.Page page(ChatLogManager log, UUID uuid, int page, int size) throws Exception {
        CompletableFuture<ChatLogManager.Page> result = new CompletableFuture<>();
        log.readPage(uuid, page, size, result::complete);
        for (int i = 0; i < 200 && !result.isDone(); i++) {
            Thread.sleep(5);
            server.getScheduler().performOneTick();
        }
        return result.get(1, TimeUnit.SECONDS);
    }

    private List<Component> runChatLog(PlayerMock viewer, String args) throws Exception {
        viewer.performCommand("chatlog " + args);
        List<Component> received = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Thread.sleep(5);
            server.getScheduler().performOneTick();
            Component next;
            while ((next = viewer.nextComponentMessage()) != null) received.add(next);
            if (!received.isEmpty() && i > 20) break;
        }
        return received;
    }

    private void record(PlayerMock player, int count) {
        for (int i = 1; i <= count; i++) {
            plugin.getChatLogManager().record(player, "msg " + i, ChatLogManager.Status.SENT);
        }
    }

    @Test
    void pagesShowNewestFirstButReadChronologically() throws Exception {
        PlayerMock bob = server.addPlayer("Bob");
        record(bob, 25);
        ChatLogManager log = plugin.getChatLogManager();

        ChatLogManager.Page first = page(log, bob.getUniqueId(), 1, 10);
        assertEquals(25, first.totalEntries());
        assertEquals(3, first.totalPages());
        assertEquals("msg 16", first.entries().get(0).message());
        assertEquals("msg 25", first.entries().get(9).message());

        ChatLogManager.Page last = page(log, bob.getUniqueId(), 3, 10);
        assertEquals(5, last.entries().size());
        assertEquals("msg 1", last.entries().get(0).message());
        assertEquals(3, page(log, bob.getUniqueId(), 99, 10).page(), "page numbers are clamped");
        assertEquals(0, page(log, UUID.randomUUID(), 1, 10).totalEntries());
    }

    @Test
    void blockedMessagesCanBeExcludedAndTabsCannotBreakTheFile() throws Exception {
        PlayerMock bob = server.addPlayer("Bob");
        plugin.getConfigManager().set("chat-log.log-blocked-messages", false);
        plugin.getChatLogManager().record(bob, "caps", ChatLogManager.Status.BLOCKED_CAPS);
        plugin.getChatLogManager().record(bob, "tab\there", ChatLogManager.Status.SENT);

        List<ChatLogManager.Entry> entries = page(plugin.getChatLogManager(), bob.getUniqueId(), 1, 10).entries();
        assertEquals(1, entries.size());
        assertEquals("tab here", entries.get(0).message());
    }

    @Test
    void oldAndSurplusEntriesAreRemoved() throws Exception {
        PlayerMock eve = server.addPlayer("Eve");
        plugin.getConfigManager().set("chat-log.max-entries-per-player", 20);
        record(eve, 60);
        ChatLogManager.Page current = page(plugin.getChatLogManager(), eve.getUniqueId(), 1, 100);
        assertEquals("msg 60", current.entries().get(current.entries().size() - 1).message());

        Path file = plugin.getDataFolder().toPath().resolve("chatlogs").resolve(eve.getUniqueId() + ".log");
        long lines = Files.lines(file).count();
        assertTrue(lines >= 20 && lines <= 30, "compacted while running, with headroom: " + lines);

        long fortyDaysAgo = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000;
        Files.writeString(file, fortyDaysAgo + "\tSENT\tEve\tancient\n" + Files.readString(file));
        assertTrue(page(plugin.getChatLogManager(), eve.getUniqueId(), 1, 100).entries().stream()
                .noneMatch(e -> e.message().equals("ancient")), "past retention-days is hidden right away");

        ChatLogManager restarted = new ChatLogManager(plugin);
        page(restarted, UUID.randomUUID(), 1, 1);
        List<String> afterRestart = Files.readAllLines(file);
        assertEquals(20, afterRestart.size());
        assertTrue(afterRestart.stream().noneMatch(l -> l.endsWith("ancient")));
        restarted.shutdown();
    }

    @Test
    void commandShowsEntriesAsPlainTextWithNavigation() throws Exception {
        PlayerMock admin = server.addPlayer("Admin");
        admin.setOp(true);
        PlayerMock bob = server.addPlayer("Bob");
        record(bob, 25);
        plugin.getChatLogManager().record(bob, "<click:run_command:'/op Bob'>free diamonds</click>",
                ChatLogManager.Status.BLOCKED_WORD_FILTER);
        inbox(admin);

        List<Component> lines = runChatLog(admin, "Bob");

        assertEquals(12, lines.size(), "header + 10 entries + navigation");
        assertTrue(PLAIN.serialize(lines.get(0)).contains("Chat log of Bob (page 1/3 - 26 entries)"));
        String newest = PLAIN.serialize(lines.get(10));
        assertTrue(newest.matches("\\[\\d\\d\\.\\d\\d\\.\\d{4} \\d\\d:\\d\\d:\\d\\d] \\[Blocked: word filter] "
                + "<click:run_command:'/op Bob'>free diamonds</click>"), newest);
        assertFalse(hasAnyClick(lines.get(10)), "logged text can't inject click events into the admin's view");
        assertTrue(hasClickCommand(lines.get(11), "/chatlog Bob 2"));
        assertFalse(hasClickCommand(lines.get(11), "/chatlog Bob 0"));
    }
}

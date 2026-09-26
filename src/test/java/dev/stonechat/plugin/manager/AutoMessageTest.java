package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoMessageTest extends PluginTestBase {

    private static final String DISCORD_LINE = "[Info] Want to chat with us on Discord? Type /discord!";

    private PlayerMock viewer;

    @BeforeEach
    void viewer() {
        viewer = server.addPlayer("Viewer");
        inbox(viewer);
    }

    private void enable(int intervalSeconds) {
        plugin.getConfigManager().set("auto-messages.enabled", true);
        plugin.getConfigManager().set("auto-messages.interval-seconds", intervalSeconds);
        plugin.reload();
    }

    private void addMultiLineMessage() {
        List<Object> messages = new ArrayList<>(plugin.getConfigManager().raw().getList("auto-messages.messages"));
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("lines", List.of("&8----------", "&7Vote daily with &e/vote", "&8----------"));
        messages.add(block);
        plugin.getConfigManager().set("auto-messages.messages", messages);
    }

    @Test
    void offByDefaultSoAnUpdateNeverStartsPostingOnItsOwn() {
        server.getScheduler().performTicks(20 * 600);
        List<String> received = inbox(viewer);
        assertTrue(received.stream().noneMatch(m -> m.contains("Discord")), received.toString());
    }

    @Test
    void postsTheDiscordReminderEveryInterval() {
        enable(60);
        server.getScheduler().performTicks(60 * 20);

        Component message = viewer.nextComponentMessage();
        assertNotNull(message);
        assertEquals(DISCORD_LINE, PLAIN.serialize(message));
        assertTrue(hasClickCommand(message, "/discord"));
        assertNull(viewer.nextComponentMessage(), "exactly one message per interval");
    }

    @Test
    void sequentialRotationWithPrefixOnlyOnTheFirstLine() {
        plugin.getAutoMessageManager().addMessage("&eSecond message");
        addMultiLineMessage();
        enable(10);

        server.getScheduler().performTicks(4 * 10 * 20);

        assertEquals(List.of(DISCORD_LINE, "[Info] Second message",
                "[Info] ----------", "Vote daily with /vote", "----------", DISCORD_LINE), inbox(viewer));
    }

    @Test
    void randomOrderNeverRepeatsTheSameMessageTwiceInARow() {
        plugin.getAutoMessageManager().addMessage("&eSecond message");
        plugin.getAutoMessageManager().addMessage("&eThird message");
        plugin.getConfigManager().set("auto-messages.order", "RANDOM");
        enable(5);

        String previous = null;
        for (int i = 0; i < 100; i++) {
            server.getScheduler().performTicks(5 * 20);
            String current = inbox(viewer).get(0);
            assertNotEquals(previous, current);
            previous = current;
        }
    }

    @Test
    void nothingIsPostedBelowTheMinimumPlayerCount() {
        plugin.getConfigManager().set("auto-messages.min-players-online", 2);
        enable(10);
        server.getScheduler().performTicks(10 * 20);
        assertTrue(inbox(viewer).isEmpty());

        server.addPlayer("Second");
        server.getScheduler().performTicks(10 * 20);
        assertEquals(List.of(DISCORD_LINE), inbox(viewer));
    }

    @Test
    void editorChangesAreSavedToConfigYml() {
        plugin.getAutoMessageManager().addMessage("&eOne more");
        plugin.getAutoMessageManager().addMessage("&eAnd another");
        plugin.getAutoMessageManager().removeLastMessage();

        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        assertEquals(2, onDisk.getList("auto-messages.messages").size());
        assertEquals(2, plugin.getConfigManager().getAutoMessages().size());
    }
}

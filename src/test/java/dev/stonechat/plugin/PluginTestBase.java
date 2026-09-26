package dev.stonechat.plugin;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Boots the real plugin inside MockBukkit with defaults that keep tests deterministic. */
public abstract class PluginTestBase {

    protected static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    protected ServerMock server;
    protected StoneChat plugin;

    @BeforeEach
    void bootPlugin() {
        server = MockBukkit.mock(new TestServerMock());
        plugin = MockBukkit.load(StoneChat.class);
        plugin.getConfigManager().set("update-checker.enabled", false);
        plugin.getConfigManager().set("join-delay.enabled", false);
        plugin.getConfigManager().set("cooldown.enabled", false);
        plugin.reload();
    }

    @AfterEach
    void shutdown() {
        MockBukkit.unmock();
    }

    /** Fires a chat message the way Paper does: as an async AsyncChatEvent on a non-main thread. */
    protected AsyncChatEvent chat(Player player, String text) throws Exception {
        Component message = Component.text(text);
        AsyncChatEvent event = new AsyncChatEvent(true, player, new HashSet<>(server.getOnlinePlayers()),
                ChatRenderer.defaultRenderer(), message, message, SignedMessage.system(text, message));
        server.getScheduler().executeAsyncEvent(event).get(5, TimeUnit.SECONDS);
        return event;
    }

    /** What a viewer would see for a delivered (not cancelled) chat event. */
    protected String rendered(AsyncChatEvent event, Player viewer) {
        return PLAIN.serialize(renderedComponent(event, viewer));
    }

    protected Component renderedComponent(AsyncChatEvent event, Player viewer) {
        return event.renderer().render(event.getPlayer(), event.getPlayer().displayName(), event.message(), viewer);
    }

    /** Drains all chat messages a player received so far, as plain text. */
    protected List<String> inbox(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        Component next;
        while ((next = player.nextComponentMessage()) != null) {
            messages.add(PLAIN.serialize(next));
        }
        return messages;
    }

    protected static boolean hasClickCommand(Component component, String command) {
        if (component.clickEvent() != null && component.clickEvent().payload() instanceof ClickEvent.Payload.Text text
                && text.value().contains(command)) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasClickCommand(child, command)) return true;
        }
        return false;
    }

    protected static boolean hasAnyClick(Component component) {
        if (component.clickEvent() != null) return true;
        for (Component child : component.children()) {
            if (hasAnyClick(child)) return true;
        }
        return false;
    }
}

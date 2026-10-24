package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.PluginTestBase;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** [item] in chat shows the item in the main hand, with its tooltip on hover. */
class ItemShowcaseTest extends PluginTestBase {

    private PlayerMock steve;
    private PlayerMock alex;

    @BeforeEach
    void players() {
        steve = server.addPlayer("Steve");
        alex = server.addPlayer("Alex");
    }

    private Component whatAlexSees(String message) throws Exception {
        AsyncChatEvent event = chat(steve, message);
        assertFalse(event.isCancelled());
        return renderedComponent(event, alex);
    }

    private static HoverEvent.ShowItem shownItem(Component component) {
        if (component.hoverEvent() != null && component.hoverEvent().value() instanceof HoverEvent.ShowItem item) {
            return item;
        }
        for (Component child : component.children()) {
            HoverEvent.ShowItem found = shownItem(child);
            if (found != null) return found;
        }
        return null;
    }

    private void hold(Material material, int amount) {
        steve.getInventory().setItemInMainHand(new ItemStack(material, amount));
    }

    @Test
    void theHeldItemIsShownWithItsTooltip() throws Exception {
        hold(Material.DIAMOND_SWORD, 1);

        Component seen = whatAlexSees("look at my [item]!");

        HoverEvent.ShowItem item = shownItem(seen);
        assertNotNull(item, "hovering shows the item");
        assertEquals("minecraft:diamond_sword", item.item().asString());
        assertFalse(PLAIN.serialize(seen).contains("[item]"));
    }

    @Test
    void stacksShowTheirAmountAndThePlaceholderIgnoresCase() throws Exception {
        hold(Material.DIAMOND, 3);

        Component seen = whatAlexSees("got [ITEM]");

        assertEquals(3, shownItem(seen).count());
        assertTrue(PLAIN.serialize(seen).contains(" x3"), PLAIN.serialize(seen));
    }

    @Test
    void anEmptyHandSaysSo() throws Exception {
        assertTrue(PLAIN.serialize(whatAlexSees("my [item]")).contains("my [empty hand]"));
    }

    @Test
    void playersCannotTypeTheItemTagThemselves() throws Exception {
        hold(Material.DIAMOND_SWORD, 1);

        assertNull(shownItem(whatAlexSees("<stonechat_item> fake")));
    }

    @Test
    void renamedItemsThatTheFiltersWouldBlockAreNotShown() throws Exception {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.customName(Component.text("badword1 blade"));
        sword.setItemMeta(meta);
        steve.getInventory().setItemInMainHand(sword);
        assertNull(shownItem(whatAlexSees("my [item]")), "a forbidden word in the name");

        meta.customName(Component.text("Blade"));
        meta.lore(List.of(Component.text("join example.com")));
        sword.setItemMeta(meta);
        steve.getInventory().setItemInMainHand(sword);
        assertNull(shownItem(whatAlexSees("my [item]")), "a link in the lore");

        steve.addAttachment(plugin, "stonechat.links.bypass", true);
        assertNotNull(shownItem(whatAlexSees("my [item]")), "players allowed to post links may show it");
    }

    @Test
    void canRequireAPermissionOrBeTurnedOff() throws Exception {
        hold(Material.DIAMOND_SWORD, 1);
        plugin.getChatFormatManager().set("item-showcase.permission", "stonechat.showcase");
        plugin.reload();
        assertNull(shownItem(whatAlexSees("my [item]")));

        steve.addAttachment(plugin, "stonechat.showcase", true);
        assertNotNull(shownItem(whatAlexSees("my [item]")));

        plugin.getChatFormatManager().set("item-showcase.enabled", false);
        plugin.reload();
        Component seen = whatAlexSees("my [item]");
        assertNull(shownItem(seen));
        assertTrue(PLAIN.serialize(seen).contains("my [item]"));
    }
}

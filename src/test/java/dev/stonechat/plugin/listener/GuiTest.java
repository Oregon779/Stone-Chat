package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.model.ChatColorGuiHolder;
import dev.stonechat.plugin.model.EditorGuiHolder;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuiTest extends PluginTestBase {

    private PlayerMock steve;

    @BeforeEach
    void player() {
        steve = server.addPlayer("Steve");
    }

    private InventoryClickEvent click(int rawSlot) {
        return steve.simulateInventoryClick(steve.getOpenInventory(), ClickType.LEFT, rawSlot);
    }

    private int slotOf(ChatColorGuiHolder holder, String colorId) {
        return holder.getSlotToColorId().entrySet().stream()
                .filter(e -> e.getValue().equals(colorId)).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    private ChatColorGuiHolder openNormalColors() {
        steve.performCommand("chat color");
        click(10);
        server.getScheduler().performOneTick();
        return assertInstanceOf(ChatColorGuiHolder.class, steve.getOpenInventory().getTopInventory().getHolder(false));
    }

    @Test
    void settingsMenuClickIsHandledOnTheNextTick() {
        steve.performCommand("chat");
        assertInstanceOf(EditorGuiHolder.class, steve.getOpenInventory().getTopInventory().getHolder(false));

        InventoryClickEvent event = click(10);

        assertTrue(event.isCancelled());
        assertFalse(plugin.getPingManager().isIgnoringPings(steve), "must not run inside InventoryClickEvent");
        server.getScheduler().performOneTick();
        assertTrue(plugin.getPingManager().isIgnoringPings(steve));
    }

    @Test
    void itemsCannotBeMovedIntoOrOutOfMenus() {
        steve.performCommand("chat");
        InventoryView view = steve.getOpenInventory();
        int bottomSlot = view.getTopInventory().getSize() + 3;

        assertTrue(steve.simulateInventoryClick(view, ClickType.SHIFT_LEFT, bottomSlot).isCancelled());
        assertTrue(click(0).isCancelled());
    }

    @Test
    void pickingAFreeColorSavesItAndClosesTheMenu() {
        ChatColorGuiHolder holder = openNormalColors();

        click(slotOf(holder, "yellow"));
        assertNull(plugin.getPlayerColorManager().getColorCode(steve.getUniqueId()));
        server.getScheduler().performOneTick();

        assertEquals("&e", plugin.getPlayerColorManager().getColorCode(steve.getUniqueId()));
        var top = steve.getOpenInventory().getTopInventory();
        assertTrue(top == null || !(top.getHolder(false) instanceof ChatColorGuiHolder), "menu is closed");
    }

    @Test
    void lockedColorIsRefused() {
        openNormalColors();
        click(42); // gold is on the second page of normal colors
        server.getScheduler().performOneTick();
        ChatColorGuiHolder holder = (ChatColorGuiHolder) steve.getOpenInventory().getTopInventory().getHolder(false);
        assertEquals(1, holder.getPage());
        inbox(steve);

        click(slotOf(holder, "gold"));
        server.getScheduler().performOneTick();

        assertNull(plugin.getPlayerColorManager().getColorCode(steve.getUniqueId()));
        assertTrue(inbox(steve).stream().anyMatch(m -> m.contains("don't have permission")));
    }
}

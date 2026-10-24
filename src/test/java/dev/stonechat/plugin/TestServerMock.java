package dev.stonechat.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.InventoryHolder;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.inventory.ChestInventoryMock;
import org.mockbukkit.mockbukkit.inventory.InventoryMock;

/**
 * Fills two gaps of MockBukkit the plugin relies on: Paper's Inventory#getHolder(boolean) and custom chat
 * completions (see {@link TestPlayerMock}).
 */
public class TestServerMock extends ServerMock {

    @Override
    public PlayerMock addPlayer(String name) {
        TestPlayerMock player = new TestPlayerMock(this, name);
        addPlayer(player);
        return player;
    }

    @Override
    public InventoryMock createInventory(InventoryHolder owner, int size, Component title) {
        return new ChestInventoryMock(owner, size) {
            @Override
            public InventoryHolder getHolder(boolean useSnapshot) {
                return getHolder();
            }
        };
    }
}

package dev.stonechat.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.InventoryHolder;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.inventory.ChestInventoryMock;
import org.mockbukkit.mockbukkit.inventory.InventoryMock;

/** MockBukkit does not implement Paper's Inventory#getHolder(boolean), which the plugin uses for performance. */
public class TestServerMock extends ServerMock {

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

package dev.stonechat.plugin.model;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class EditorGuiHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, BiConsumer<Player, ClickType>> handlers = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setHandler(int slot, BiConsumer<Player, ClickType> handler) {
        handlers.put(slot, handler);
    }

    public BiConsumer<Player, ClickType> getHandler(int slot) {
        return handlers.get(slot);
    }
}

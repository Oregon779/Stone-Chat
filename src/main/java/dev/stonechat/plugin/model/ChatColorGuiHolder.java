package dev.stonechat.plugin.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class ChatColorGuiHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, String> slotToColorId = new HashMap<>();
    private String category = "NORMAL";
    private int page;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, String> getSlotToColorId() {
        return slotToColorId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}

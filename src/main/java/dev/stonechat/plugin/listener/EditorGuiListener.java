package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.model.EditorGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.BiConsumer;

public class EditorGuiListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EditorGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !(clicked.getHolder() instanceof EditorGuiHolder)) {
            return;
        }

        BiConsumer<Player, org.bukkit.event.inventory.ClickType> handler = holder.getHandler(event.getSlot());
        if (handler != null) {
            handler.accept(player, event.getClick());
        }
    }
}

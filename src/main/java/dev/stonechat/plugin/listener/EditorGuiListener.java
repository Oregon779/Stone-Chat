package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.model.EditorGuiHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public class EditorGuiListener implements Listener {

    private final Plugin plugin;

    public EditorGuiListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof EditorGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof EditorGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return; // outside the window or in the player's own inventory
        }

        BiConsumer<Player, ClickType> handler = holder.getHandler(rawSlot);
        if (handler != null) {
            ClickType click = event.getClick();
            // Handlers open/close inventories, which Bukkit forbids inside InventoryClickEvent itself.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) handler.accept(player, click);
            });
        }
    }
}

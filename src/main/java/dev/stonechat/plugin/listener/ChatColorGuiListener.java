package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatColorGuiManager;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.model.ChatColorGuiHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ChatColorGuiListener implements Listener {

    private final StoneChat plugin;

    public ChatColorGuiListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof ChatColorGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof ChatColorGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return; // outside the window or in the player's own inventory
        }

        String colorId = holder.getSlotToColorId().get(rawSlot);
        if (colorId == null) return;

        // Opening/closing inventories is not allowed inside InventoryClickEvent itself.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) handleClick(player, holder, colorId);
        });
    }

    private void handleClick(Player player, ChatColorGuiHolder holder, String colorId) {
        ChatColorGuiManager gui = plugin.getChatColorGuiManager();
        switch (colorId) {
            case ChatColorGuiManager.BACK_BUTTON_ID -> {
                gui.returnToHub(player);
                return;
            }
            case ChatColorGuiManager.PREV_PAGE_ID -> {
                gui.openColorList(player, ChatColorGuiManager.ColorCategory.valueOf(holder.getCategory()), holder.getPage() - 1);
                return;
            }
            case ChatColorGuiManager.NEXT_PAGE_ID -> {
                gui.openColorList(player, ChatColorGuiManager.ColorCategory.valueOf(holder.getCategory()), holder.getPage() + 1);
                return;
            }
            default -> {
            }
        }

        ChatColorGuiManager.ColorOption option = gui.findOption(colorId);
        if (option == null) return;

        if (!option.permission().isBlank() && !player.hasPermission(option.permission())) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("chat-color.no-permission-color"));
            plugin.getSoundManager().play(player, "chat-color-gui.sound-denied");
            return;
        }

        if (option.isReset()) {
            plugin.getPlayerColorManager().clearColor(player.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().getPrefixed("chat-color.reset"));
        } else {
            plugin.getPlayerColorManager().setColor(player.getUniqueId(), option.colorCode());
            player.sendMessage(plugin.getLanguageManager().getPrefixed("chat-color.selected",
                    LanguageManager.placeholders("%colorname%", option.displayName())));
        }

        plugin.getSoundManager().play(player, "chat-color-gui.sound-select");
        player.closeInventory();
    }
}

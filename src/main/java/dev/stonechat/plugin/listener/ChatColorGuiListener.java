package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatColorGuiManager;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.model.ChatColorGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class ChatColorGuiListener implements Listener {

    private final StoneChat plugin;

    public ChatColorGuiListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ChatColorGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ChatColorGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof ChatColorGuiHolder)) {
            return;
        }

        String colorId = holder.getSlotToColorId().get(event.getSlot());
        if (colorId == null) return;

        if (colorId.equals(ChatColorGuiManager.BACK_BUTTON_ID)) {
            plugin.getChatColorGuiManager().returnToHub(player);
            return;
        }
        if (colorId.equals(ChatColorGuiManager.PREV_PAGE_ID)) {
            plugin.getChatColorGuiManager().openColorList(player,
                    ChatColorGuiManager.ColorCategory.valueOf(holder.getCategory()), holder.getPage() - 1);
            return;
        }
        if (colorId.equals(ChatColorGuiManager.NEXT_PAGE_ID)) {
            plugin.getChatColorGuiManager().openColorList(player,
                    ChatColorGuiManager.ColorCategory.valueOf(holder.getCategory()), holder.getPage() + 1);
            return;
        }

        ChatColorGuiManager.ColorOption option = plugin.getChatColorGuiManager().loadOptions().stream()
                .filter(o -> o.id().equals(colorId))
                .findFirst()
                .orElse(null);
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

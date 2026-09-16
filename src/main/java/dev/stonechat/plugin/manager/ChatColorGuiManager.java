package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.model.ChatColorGuiHolder;
import dev.stonechat.plugin.model.EditorGuiHolder;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatColorGuiManager {

    public static final String BACK_BUTTON_ID = "__back__";
    public static final String PREV_PAGE_ID = "__prev_page__";
    public static final String NEXT_PAGE_ID = "__next_page__";

    private final StoneChat plugin;

    private final java.util.Set<java.util.UUID> openedFromSettings = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ChatColorGuiManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public record ColorOption(String id, String displayName, String colorCode, Material material, String permission, boolean premium) {

        public boolean isReset() {
            return colorCode == null || colorCode.isBlank();
        }

        public boolean isGradient() {
            return colorCode != null && colorCode.contains("<gradient");
        }
    }

    public List<ColorOption> loadOptions() {
        List<ColorOption> options = new ArrayList<>();
        ConfigurationSection section = plugin.getConfigManager().getSection("chat-color-gui.colors");
        if (section == null) return options;

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String displayName = entry.getString("display-name", id);
            String colorCode = entry.getString("color-code", "");
            String permission = entry.getString("permission", "");
            Material material = parseMaterial(entry.getString("material", "WHITE_DYE"));
            boolean premium = entry.getBoolean("premium", false);

            options.add(new ColorOption(id, displayName, colorCode, material, permission, premium));
        }
        return options;
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + name + "' configured for a chat color option, falling back to WHITE_DYE.");
            return Material.WHITE_DYE;
        }
    }

    public void clearState(java.util.UUID uuid) {
        openedFromSettings.remove(uuid);
    }

    public void open(Player player) {
        if (!checkAccessible(player)) return;
        openedFromSettings.remove(player.getUniqueId());
        openHub(player);
    }

    public void openFromSettings(Player player) {
        if (!checkAccessible(player)) return;
        openedFromSettings.add(player.getUniqueId());
        openHub(player);
    }

    /** @return true if the player is allowed to open the GUI right now; sends them the appropriate denial message otherwise. */
    private boolean checkAccessible(Player player) {
        if (!plugin.getConfigManager().isChatColorGuiEnabled()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("chat-color.gui-disabled"));
            return false;
        }
        String usePermission = plugin.getConfigManager().getChatColorGuiUsePermission();
        if (usePermission != null && !usePermission.isBlank() && !player.hasPermission(usePermission)) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return false;
        }
        return true;
    }

    public void returnToHub(Player player) {
        openHub(player);
    }

    private void openHub(Player player) {
        var gui = plugin.getGuiConfigManager();
        var source = gui.chatColorGui();

        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                gui.title(source, "hub.title", "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>Chat Color</bold></gradient> <dark_gray>-"));
        holder.setInventory(inventory);

        inventory.setItem(10, gui.buildItem(source, "hub.normal-button", Material.WHITE_DYE));
        holder.setHandler(10, (p, click) -> openColorList(p, ColorCategory.NORMAL, 0));

        inventory.setItem(13, gui.buildItem(source, "hub.gradient-button", Material.FIREWORK_STAR));
        holder.setHandler(13, (p, click) -> openColorList(p, ColorCategory.GRADIENT, 0));

        inventory.setItem(16, gui.buildItem(source, "hub.premium-button", Material.NETHER_STAR));
        holder.setHandler(16, (p, click) -> openColorList(p, ColorCategory.PREMIUM, 0));

        if (openedFromSettings.contains(player.getUniqueId())) {
            inventory.setItem(26, gui.buildNavItem(source, "hub.back-to-settings-button", Material.ARROW, "&7« Back to Settings"));
            holder.setHandler(26, (p, click) -> plugin.getPlayerSettingsGuiManager().open(p));
        }

        fillEmptySlots(inventory, 27);
        player.openInventory(inventory);
    }

    public enum ColorCategory { NORMAL, GRADIENT, PREMIUM }

    public void openColorList(Player player, ColorCategory category, int page) {
        var gui = plugin.getGuiConfigManager();
        var source = gui.chatColorGui();

        List<ColorOption> allOptions = loadOptions().stream()
                .filter(o -> o.isReset() || matchesCategory(o, category))
                .toList();

        int perPage = 18;
        int totalPages = Math.max(1, (int) Math.ceil(allOptions.size() / (double) perPage));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        List<ColorOption> options = allOptions.subList(
                clampedPage * perPage, Math.min(allOptions.size(), (clampedPage + 1) * perPage));

        String titlePath = switch (category) {
            case GRADIENT -> "list.title-gradient";
            case PREMIUM -> "list.title-premium";
            default -> "list.title-normal";
        };
        String titleFallback = switch (category) {
            case GRADIENT -> "Gradient Colors";
            case PREMIUM -> "Premium Colors";
            default -> "Normal Colors";
        };
        Component title = ColorUtil.parse(source.getString(titlePath, titleFallback) + " &7(" + (clampedPage + 1) + "/" + totalPages + ")");

        int size = 45;
        ChatColorGuiHolder holder = new ChatColorGuiHolder();
        holder.setCategory(category.name());
        holder.setPage(clampedPage);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        String currentColorCode = plugin.getPlayerColorManager().getColorCode(player.getUniqueId());

        int[] gridColumns = {1, 2, 3, 5, 6, 7};
        int index = 0;
        int row = 1;
        while (index < options.size()) {
            int remaining = options.size() - index;
            int itemsThisRow = Math.min(gridColumns.length, remaining);
            boolean fullRow = itemsThisRow == gridColumns.length;
            int plainStartCol = 1 + (7 - itemsThisRow) / 2;

            for (int i = 0; i < itemsThisRow; i++) {
                int col = fullRow ? gridColumns[i] : plainStartCol + i;
                int slot = row * 9 + col;
                ColorOption option = options.get(index);

                ItemStack item = new ItemStack(option.material());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(ColorUtil.parse(option.displayName()));

                    boolean locked = !option.permission().isBlank() && !player.hasPermission(option.permission());
                    boolean selected = isSelected(currentColorCode, option);

                    List<Component> lore = new ArrayList<>();
                    if (locked) {
                        lore.add(plugin.getLanguageManager().get("chat-color.locked-lore"));
                    } else if (selected) {
                        lore.add(plugin.getLanguageManager().get("chat-color.selected-lore"));
                    } else {
                        lore.add(plugin.getLanguageManager().get("chat-color.select-lore"));
                    }
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }

                inventory.setItem(slot, item);
                holder.getSlotToColorId().put(slot, option.id());
                index++;
            }
            row++;
        }

        if (clampedPage > 0) {
            ItemStack prevItem = gui.buildNavItem(source, "list.prev-page-button", Material.ARROW, "&e« Previous Page");
            inventory.setItem(38, prevItem);
            holder.getSlotToColorId().put(38, PREV_PAGE_ID);
        }
        if (clampedPage < totalPages - 1) {
            ItemStack nextItem = gui.buildNavItem(source, "list.next-page-button", Material.ARROW, "&eNext Page »");
            inventory.setItem(42, nextItem);
            holder.getSlotToColorId().put(42, NEXT_PAGE_ID);
        }

        ItemStack backItem = gui.buildNavItem(source, "list.back-button", Material.BARRIER, "&7« Back");
        inventory.setItem(44, backItem);
        holder.getSlotToColorId().put(44, BACK_BUTTON_ID);

        fillEmptySlots(inventory, size);
        player.openInventory(inventory);
    }

    private void fillEmptySlots(Inventory inventory, int size) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(" "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private boolean matchesCategory(ColorOption option, ColorCategory category) {
        return switch (category) {
            case PREMIUM -> option.premium();
            case GRADIENT -> option.isGradient() && !option.premium();
            case NORMAL -> !option.isGradient() && !option.premium();
        };
    }

    private boolean isSelected(String currentColorCode, ColorOption option) {
        String normalizedCurrent = currentColorCode == null ? "" : currentColorCode;
        String normalizedOption = option.colorCode() == null ? "" : option.colorCode();
        return Objects.equals(normalizedCurrent, normalizedOption);
    }
}

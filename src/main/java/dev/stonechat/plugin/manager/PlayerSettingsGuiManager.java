package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.model.EditorGuiHolder;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A small, player-facing settings menu (unlike {@link SettingsEditorManager},
 * which is admin-only): lets any player toggle whether they receive @pings,
 * jump straight to the chat color picker, and manage who they ignore.
 * Every title, button material, name and lore line is read from
 * gui/settings-gui.yml via {@link GuiConfigManager} - nothing here is
 * hardcoded, so admins can restyle this menu completely without touching Java.
 */
public class PlayerSettingsGuiManager {

    private static final int[] GRID_COLUMNS = {1, 2, 3, 5, 6, 7};
    private static final int PER_PAGE = 18;

    private final StoneChat plugin;

    public PlayerSettingsGuiManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    private YamlConfiguration gui() {
        return plugin.getGuiConfigManager().settingsGui();
    }

    public void open(Player player) {
        var gui = plugin.getGuiConfigManager();
        YamlConfiguration source = gui();

        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                gui.title(source, "main.title", "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>My Stone Chat Settings</bold></gradient> <dark_gray>-"));
        holder.setInventory(inv);

        boolean ignoringPings = plugin.getPingManager().isIgnoringPings(player);
        inv.setItem(10, gui.buildStatefulItem(source, "main.ping-button", !ignoringPings, Material.LIME_DYE, Material.GRAY_DYE));
        holder.setHandler(10, (p, click) -> {
            boolean nowIgnoring = plugin.getPingManager().toggleIgnorePings(p);
            p.sendMessage(plugin.getLanguageManager().getPrefixed(nowIgnoring ? "ping.toggled-on" : "ping.toggled-off"));
            open(p);
        });

        inv.setItem(13, gui.buildItem(source, "main.color-button", Material.FIREWORK_STAR));
        holder.setHandler(13, (p, click) -> plugin.getChatColorGuiManager().openFromSettings(p));

        Set<UUID> ignored = plugin.getIgnoreManager().getIgnored(player.getUniqueId());
        inv.setItem(16, gui.buildItem(source, "main.ignore-button", Material.BARRIER, "%count%", String.valueOf(ignored.size())));
        holder.setHandler(16, (p, click) -> openIgnoreHub(p));

        fillEmptySlots(inv, 27);
        player.openInventory(inv);
    }

    private void openIgnoreHub(Player player) {
        var gui = plugin.getGuiConfigManager();
        YamlConfiguration source = gui();

        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                gui.title(source, "ignore-hub.title", "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>Manage Ignored Players</bold></gradient> <dark_gray>-"));
        holder.setInventory(inv);

        int count = plugin.getIgnoreManager().getIgnored(player.getUniqueId()).size();
        inv.setItem(11, gui.buildItem(source, "ignore-hub.list-button", Material.BOOK, "%count%", String.valueOf(count)));
        holder.setHandler(11, (p, click) -> openMyIgnoreList(p, 0));

        inv.setItem(15, gui.buildItem(source, "ignore-hub.add-button", Material.PLAYER_HEAD));
        holder.setHandler(15, (p, click) -> openIgnorePicker(p, "", 0));

        inv.setItem(26, gui.buildNavItem(source, "ignore-hub.back-button", Material.ARROW, "&7« Back"));
        holder.setHandler(26, (p, click) -> open(p));

        fillEmptySlots(inv, 27);
        player.openInventory(inv);
    }

    private void openMyIgnoreList(Player player, int page) {
        var gui = plugin.getGuiConfigManager();
        YamlConfiguration source = gui();

        List<UUID> ignored = new ArrayList<>(plugin.getIgnoreManager().getIgnored(player.getUniqueId()));
        ignored.sort(Comparator.comparing(this::nameOf));

        int totalPages = Math.max(1, (int) Math.ceil(ignored.size() / (double) PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        List<UUID> pageItems = ignored.subList(clampedPage * PER_PAGE, Math.min(ignored.size(), (clampedPage + 1) * PER_PAGE));

        EditorGuiHolder holder = new EditorGuiHolder();
        String titleBase = source.getString("my-ignore-list.title", "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>My Ignore List</bold></gradient> <dark_gray>-");
        Inventory inv = Bukkit.createInventory(holder, 45,
                ColorUtil.parse(titleBase + " &7(" + (clampedPage + 1) + "/" + totalPages + ")"));
        holder.setInventory(inv);

        if (pageItems.isEmpty()) {
            inv.setItem(22, gui.buildItem(source, "my-ignore-list.empty-item", Material.BARRIER));
        } else {
            List<String> entryLore = nonEmpty(source.getStringList("my-ignore-list.entry-lore"), "&7Click to stop ignoring this player");
            placeGrid(pageItems.size(), 1, (i, row, col) -> {
                UUID uuid = pageItems.get(i);
                String name = nameOf(uuid);
                ItemStack head = buildSkull(uuid, "&c" + name, entryLore);
                int slot = row * 9 + col;
                inv.setItem(slot, head);
                holder.setHandler(slot, (p, click) -> {
                    plugin.getIgnoreManager().toggleIgnore(p.getUniqueId(), uuid);
                    p.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.no-longer-ignoring",
                            LanguageManager.placeholders("%player%", name)));
                    openMyIgnoreList(p, clampedPage);
                });
            });
        }

        if (clampedPage > 0) {
            inv.setItem(37, gui.buildNavItem(source, "nav.prev-page-button", Material.ARROW, "&e« Previous Page"));
            holder.setHandler(37, (p, click) -> openMyIgnoreList(player, clampedPage - 1));
        }
        if (clampedPage < totalPages - 1) {
            inv.setItem(42, gui.buildNavItem(source, "nav.next-page-button", Material.ARROW, "&eNext Page »"));
            holder.setHandler(42, (p, click) -> openMyIgnoreList(player, clampedPage + 1));
        }
        inv.setItem(44, gui.buildNavItem(source, "nav.back-button", Material.ARROW, "&7« Back"));
        holder.setHandler(44, (p, click) -> openIgnoreHub(player));

        fillEmptySlots(inv, 45);
        player.openInventory(inv);
    }

    private void openIgnorePicker(Player player, String search, int page) {
        var gui = plugin.getGuiConfigManager();
        YamlConfiguration source = gui();

        String lowerSearch = search.toLowerCase();
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (!lowerSearch.isBlank() && !online.getName().toLowerCase().contains(lowerSearch)) continue;
            candidates.add(online);
        }
        candidates.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil(candidates.size() / (double) PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        List<Player> pageItems = candidates.subList(clampedPage * PER_PAGE, Math.min(candidates.size(), (clampedPage + 1) * PER_PAGE));

        EditorGuiHolder holder = new EditorGuiHolder();
        String titleBase = source.getString("ignore-picker.title", "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>Ignore Someone</bold></gradient> <dark_gray>-");
        String titleSuffix = search.isBlank() ? "" : " &7- \"" + search + "\"";
        Inventory inv = Bukkit.createInventory(holder, 45,
                ColorUtil.parse(titleBase + " &7(" + (clampedPage + 1) + "/" + totalPages + ")" + titleSuffix));
        holder.setInventory(inv);

        inv.setItem(4, gui.buildItem(source, "ignore-picker.search-button", Material.NAME_TAG));
        holder.setHandler(4, (p, click) -> plugin.getAnvilInputManager().openTextInput(p, "Search Players", search,
                newSearch -> openIgnorePicker(p, newSearch.equalsIgnoreCase("cancel") ? search : newSearch, 0)));

        if (pageItems.isEmpty()) {
            inv.setItem(22, gui.buildItem(source, "ignore-picker.empty-item", Material.BARRIER));
        } else {
            List<String> loreNotIgnored = nonEmpty(source.getStringList("ignore-picker.entry-lore-not-ignored"), "&7Click to ignore this player");
            List<String> loreIgnored = nonEmpty(source.getStringList("ignore-picker.entry-lore-ignored"), "&a✔ Already ignoring - click to un-ignore");
            List<String> loreImmune = nonEmpty(source.getStringList("ignore-picker.entry-lore-immune"), "&7This player can't be ignored.");

            placeGrid(pageItems.size(), 2, (i, row, col) -> {
                Player target = pageItems.get(i);
                boolean alreadyIgnored = plugin.getIgnoreManager().isIgnoring(player.getUniqueId(), target.getUniqueId());
                boolean immune = target.hasPermission("stonechat.ignore.immune");
                List<String> lore = immune ? loreImmune : (alreadyIgnored ? loreIgnored : loreNotIgnored);

                ItemStack head = buildSkull(target.getUniqueId(), (alreadyIgnored ? "&7" : "&f") + target.getName(), lore);
                int slot = row * 9 + col;
                inv.setItem(slot, head);
                holder.setHandler(slot, (p, click) -> {
                    if (immune) return;
                    plugin.getIgnoreManager().toggleIgnore(p.getUniqueId(), target.getUniqueId());
                    p.sendMessage(plugin.getLanguageManager().getPrefixed(
                            alreadyIgnored ? "ignore.no-longer-ignoring" : "ignore.now-ignoring",
                            LanguageManager.placeholders("%player%", target.getName())));
                    openIgnorePicker(p, search, clampedPage);
                });
            });
        }

        if (clampedPage > 0) {
            inv.setItem(37, gui.buildNavItem(source, "nav.prev-page-button", Material.ARROW, "&e« Previous Page"));
            holder.setHandler(37, (p, click) -> openIgnorePicker(player, search, clampedPage - 1));
        }
        if (clampedPage < totalPages - 1) {
            inv.setItem(42, gui.buildNavItem(source, "nav.next-page-button", Material.ARROW, "&eNext Page »"));
            holder.setHandler(42, (p, click) -> openIgnorePicker(player, search, clampedPage + 1));
        }
        inv.setItem(44, gui.buildNavItem(source, "nav.back-button", Material.ARROW, "&7« Back"));
        holder.setHandler(44, (p, click) -> openIgnoreHub(player));

        fillEmptySlots(inv, 45);
        player.openInventory(inv);
    }

    private List<String> nonEmpty(List<String> lines, String fallback) {
        return lines.isEmpty() ? List.of(fallback) : lines;
    }

    @FunctionalInterface
    private interface IndexedGridConsumer {
        void accept(int index, int row, int col);
    }

    private void placeGrid(int count, int startRow, IndexedGridConsumer placer) {
        int index = 0;
        int row = startRow;
        while (index < count) {
            int remaining = count - index;
            int itemsThisRow = Math.min(GRID_COLUMNS.length, remaining);
            if (itemsThisRow == GRID_COLUMNS.length) {
                for (int i = 0; i < itemsThisRow; i++) {
                    placer.accept(index, row, GRID_COLUMNS[i]);
                    index++;
                }
            } else {
                int startCol = 1 + (7 - itemsThisRow) / 2;
                for (int i = 0; i < itemsThisRow; i++) {
                    placer.accept(index, row, startCol + i);
                    index++;
                }
            }
            row++;
        }
    }

    private String nameOf(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private ItemStack buildSkull(UUID uuid, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            skullMeta.displayName(ColorUtil.parse(name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ColorUtil.parse(line).decoration(TextDecoration.ITALIC, false));
            }
            skullMeta.lore(lore);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    private void fillEmptySlots(Inventory inv, int size) {
        ItemStack filler = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        var meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(" "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }
}

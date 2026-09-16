package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.config.ConfigUpdater;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and serves the two fully admin-customizable GUI layout files:
 * gui/chatcolor-gui.yml (the /chat color hub and color-list screens) and
 * gui/settings-gui.yml (the player /chat settings menu and its ignore
 * submenus). Every title, item material, display name and lore line in
 * those two GUIs is read from here instead of being hardcoded in Java.
 */
public class GuiConfigManager {

    private final StoneChat plugin;
    private volatile YamlConfiguration chatColorGui;
    private volatile YamlConfiguration settingsGui;

    public GuiConfigManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File chatColorFile = new File(plugin.getDataFolder(), "gui/chatcolor-gui.yml");
        chatColorGui = ConfigUpdater.updateFile(plugin, chatColorFile, "gui/chatcolor-gui.yml");

        File settingsFile = new File(plugin.getDataFolder(), "gui/settings-gui.yml");
        settingsGui = ConfigUpdater.updateFile(plugin, settingsFile, "gui/settings-gui.yml");
    }

    public YamlConfiguration chatColorGui() {
        return chatColorGui;
    }

    public YamlConfiguration settingsGui() {
        return settingsGui;
    }

    /** Reads a title string (MiniMessage/legacy mixed) at {@code path} from {@code source} and parses it into a Component. */
    public Component title(YamlConfiguration source, String path, String fallback) {
        return ColorUtil.parse(source.getString(path, fallback));
    }

    /**
     * Builds an ItemStack from a "material / name / lore" block at {@code path} in {@code source}.
     * Placeholder pairs (e.g. "%count%", "5") are applied to every lore line before parsing.
     */
    public ItemStack buildItem(YamlConfiguration source, String path, Material fallbackMaterial, String... placeholderPairs) {
        Material material = resolveMaterial(source.getString(path + ".material", null), fallbackMaterial);
        String name = source.getString(path + ".name", "&fUnnamed");
        List<String> loreLines = source.getStringList(path + ".lore");
        return build(material, applyPlaceholders(name, placeholderPairs), applyPlaceholders(loreLines, placeholderPairs));
    }

    /** Same as {@link #buildItem}, but reads a two-state (on/off) block with "-on"/"-off" suffixed keys. */
    public ItemStack buildStatefulItem(YamlConfiguration source, String path, boolean state,
                                        Material fallbackOnMaterial, Material fallbackOffMaterial, String... placeholderPairs) {
        String suffix = state ? "-on" : "-off";
        Material fallbackMaterial = state ? fallbackOnMaterial : fallbackOffMaterial;
        Material material = resolveMaterial(source.getString(path + ".material" + suffix, null), fallbackMaterial);
        String name = source.getString(path + ".name" + suffix, "&fUnnamed");
        List<String> loreLines = source.getStringList(path + ".lore" + suffix);
        return build(material, applyPlaceholders(name, placeholderPairs), applyPlaceholders(loreLines, placeholderPairs));
    }

    /** Builds a simple item straight from lore lines already resolved by the caller (e.g. per-entry lore built dynamically). */
    public ItemStack buildItemWithLore(YamlConfiguration source, String path, Material fallbackMaterial, String name, List<String> loreLines) {
        Material material = resolveMaterial(source.getString(path + ".material", null), fallbackMaterial);
        return build(material, name, loreLines);
    }

    /** Reads just the "name" at {@code path}, for tiny nav items (back/prev/next buttons) that don't need lore. */
    public ItemStack buildNavItem(YamlConfiguration source, String path, Material fallbackMaterial, String fallbackName) {
        Material material = resolveMaterial(source.getString(path + ".material", null), fallbackMaterial);
        String name = source.getString(path + ".name", fallbackName);
        return build(material, name, List.of());
    }

    private ItemStack build(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ColorUtil.parse(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material resolveMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Material.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + raw + "' in a gui/*.yml file - using " + fallback.name() + " instead.");
            return fallback;
        }
    }

    private String applyPlaceholders(String text, String[] pairs) {
        String result = text;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    private List<String> applyPlaceholders(List<String> lines, String[] pairs) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(applyPlaceholders(line, pairs));
        }
        return result;
    }
}

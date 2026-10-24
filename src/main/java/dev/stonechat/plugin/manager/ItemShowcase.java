package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writing [item] in chat shows the item in the main hand: its name, with the full item tooltip on hover.
 * The placeholder becomes a MiniMessage tag that players cannot type themselves, because every "<" a
 * player writes is defused by {@link ColorUtil#sanitizeUserInput}.
 */
public class ItemShowcase {

    /** The chat message with the placeholder swapped for the item tag, and the resolver that fills it in. */
    public record Prepared(String message, TagResolver resolver) {
    }

    private static final String TAG = "stonechat_item";

    private final StoneChat plugin;

    public ItemShowcase(StoneChat plugin) {
        this.plugin = plugin;
    }

    public Prepared prepare(Player player, String message) {
        ChatFormatManager format = plugin.getChatFormatManager();
        Pattern pattern = format.getItemShowcasePattern();
        if (!format.isItemShowcaseEnabled() || pattern == null || !pattern.matcher(message).find()) {
            return unchanged(message);
        }

        String permission = format.getItemShowcasePermission();
        if (!permission.isBlank() && !player.hasPermission(permission)) return unchanged(message);

        Component shown = itemComponent(player);
        if (shown == null) return unchanged(message);
        return new Prepared(pattern.matcher(message).replaceAll(Matcher.quoteReplacement("<" + TAG + ">")),
                Placeholder.component(TAG, shown));
    }

    /** {@code null} when the item must not be shown. */
    private Component itemComponent(Player player) {
        // Chat runs off the main thread: work on a copy instead of the live inventory item.
        ItemStack item = player.getInventory().getItemInMainHand().clone();
        LanguageManager lm = plugin.getLanguageManager();
        if (item.getType().isAir()) {
            return ColorUtil.parse(lm.getRaw("chat-format.item-empty-hand"));
        }
        if (isBlockedByFilters(player, item)) {
            return null;
        }

        Component name = item.displayName();
        if (item.getAmount() > 1) {
            name = name.append(ColorUtil.parse(lm.getRaw("chat-format.item-amount",
                    LanguageManager.placeholders("%amount%", String.valueOf(item.getAmount())))));
        }
        return name;
    }

    /**
     * A renamed item could carry a forbidden word or a link past the filters, in its name or its lore.
     * Only text a player can set is checked, not the built-in item name.
     */
    private boolean isBlockedByFilters(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        StringBuilder text = new StringBuilder();
        if (meta.hasCustomName() && meta.customName() != null) {
            text.append(plain.serialize(meta.customName())).append(' ');
        }
        if (meta.hasLore() && meta.lore() != null) {
            meta.lore().forEach(line -> text.append(plain.serialize(line)).append(' '));
        }
        if (text.isEmpty()) return false;

        String playerText = text.toString();
        return plugin.getWordFilterManager().check(playerText).triggered
                || (!player.hasPermission(plugin.getConfigManager().getLinkBypassPermission())
                && plugin.getLinkFilterManager().containsLink(playerText));
    }

    private static Prepared unchanged(String message) {
        return new Prepared(message, TagResolver.empty());
    }
}

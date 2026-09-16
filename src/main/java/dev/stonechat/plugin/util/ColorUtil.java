package dev.stonechat.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final String[][] LEGACY_TO_TAG = {
            {"&0", "<black>"}, {"&1", "<dark_blue>"}, {"&2", "<dark_green>"}, {"&3", "<dark_aqua>"},
            {"&4", "<dark_red>"}, {"&5", "<dark_purple>"}, {"&6", "<gold>"}, {"&7", "<gray>"},
            {"&8", "<dark_gray>"}, {"&9", "<blue>"}, {"&a", "<green>"}, {"&b", "<aqua>"},
            {"&c", "<red>"}, {"&d", "<light_purple>"}, {"&e", "<yellow>"}, {"&f", "<white>"},
            {"&k", "<obfuscated>"}, {"&l", "<bold>"}, {"&m", "<strikethrough>"}, {"&n", "<underlined>"},
            {"&o", "<italic>"}, {"&r", "<reset>"}
    };

    private ColorUtil() {
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(toMiniMessageString(raw));
    }

    public static Component parse(String raw, java.util.Map<String, String> placeholders) {
        String result = raw;
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return parse(result);
    }

    public static String toMiniMessageString(String raw) {
        String converted = HEX_PATTERN.matcher(raw).replaceAll("<#$1>");

        for (String[] pair : LEGACY_TO_TAG) {
            converted = converted.replace(pair[0], pair[1]);
            converted = converted.replace(pair[0].toUpperCase(), pair[1]);
        }
        return converted;
    }

    public static String sanitizeUserInput(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&\u200B").replace("<", "<\u200B");
    }

    public static String stripToPlain(String raw) {
        Component component = parse(raw);
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String toLegacyString(Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }
}

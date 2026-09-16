package dev.stonechat.plugin.util;

import java.util.Map;

/**
 * Converts plain Latin text into small-caps Unicode glyphs (a-z map to
 * dedicated small-caps codepoints; everything else - digits, punctuation,
 * spaces, non-Latin characters - passes through unchanged). Used purely for
 * visual styling of editor headers/titles; never applied to user-generated
 * content like chat messages or player-typed values.
 */
public final class SmallCaps {

    private static final Map<Character, Character> MAP = Map.ofEntries(
            Map.entry('a', 'ᴀ'), Map.entry('b', 'ʙ'), Map.entry('c', 'ᴄ'), Map.entry('d', 'ᴅ'),
            Map.entry('e', 'ᴇ'), Map.entry('f', 'ꜰ'), Map.entry('g', 'ɢ'), Map.entry('h', 'ʜ'),
            Map.entry('i', 'ɪ'), Map.entry('j', 'ᴊ'), Map.entry('k', 'ᴋ'), Map.entry('l', 'ʟ'),
            Map.entry('m', 'ᴍ'), Map.entry('n', 'ɴ'), Map.entry('o', 'ᴏ'), Map.entry('p', 'ᴘ'),
            Map.entry('q', 'ǫ'), Map.entry('r', 'ʀ'), Map.entry('s', 'ꜱ'), Map.entry('t', 'ᴛ'),
            Map.entry('u', 'ᴜ'), Map.entry('v', 'ᴠ'), Map.entry('w', 'ᴡ'), Map.entry('x', 'x'),
            Map.entry('y', 'ʏ'), Map.entry('z', 'ᴢ')
    );

    private SmallCaps() {
    }

    /** Converts a-z (case-insensitively) to small-caps glyphs; everything else is left untouched. */
    public static String of(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            Character mapped = MAP.get(Character.toLowerCase(c));
            result.append(mapped != null ? mapped : c);
        }
        return result.toString();
    }
}

package dev.stonechat.plugin.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\u2060\\uFEFF\\u00AD\\u180E]");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{Mn}+");
    private static final Map<Character, Character> LOOK_ALIKES = buildLookAlikeMap();

    private TextNormalizer() {
    }

    public static String normalize(String input, boolean normalizeLeetspeak) {
        if (input == null || input.isEmpty()) return "";

        String result = ZERO_WIDTH.matcher(input).replaceAll("");

        result = Normalizer.normalize(result, Normalizer.Form.NFKD);
        result = COMBINING_MARKS.matcher(result).replaceAll("");

        StringBuilder mapped = new StringBuilder(result.length());
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            mapped.append(LOOK_ALIKES.getOrDefault(c, c));
        }
        result = mapped.toString();

        if (normalizeLeetspeak) {
            result = result
                    .replace('4', 'a').replace('@', 'a')
                    .replace('3', 'e')
                    .replace('1', 'i').replace('!', 'i')
                    .replace('0', 'o')
                    .replace('5', 's').replace('$', 's')
                    .replace('7', 't').replace('+', 't');
        }

        return result.toLowerCase();
    }

    private static Map<Character, Character> buildLookAlikeMap() {
        Map<Character, Character> map = new HashMap<>();

        putBoth(map, 'а', 'a');
        putBoth(map, 'е', 'e');
        putBoth(map, 'о', 'o');
        putBoth(map, 'р', 'p');
        putBoth(map, 'с', 'c');
        putBoth(map, 'х', 'x');
        putBoth(map, 'у', 'y');
        putBoth(map, 'к', 'k');
        putBoth(map, 'м', 'm');
        putBoth(map, 'н', 'h');
        putBoth(map, 'т', 't');
        putBoth(map, 'в', 'b');
        putBoth(map, 'і', 'i');
        map.put('ѕ', 's');
        map.put('ј', 'j');
        map.put('ԁ', 'd');
        map.put('ց', 'g');

        putBoth(map, 'α', 'a');
        putBoth(map, 'ο', 'o');
        putBoth(map, 'ρ', 'p');
        putBoth(map, 'τ', 't');
        map.put('υ', 'u');
        map.put('ν', 'v');
        map.put('β', 'b');
        map.put('ε', 'e');
        map.put('η', 'n');
        map.put('κ', 'k');

        for (char c = 'Ａ'; c <= 'Ｚ'; c++) {
            map.put(c, (char) ('a' + (c - 'Ａ')));
        }
        for (char c = 'ａ'; c <= 'ｚ'; c++) {
            map.put(c, (char) ('a' + (c - 'ａ')));
        }

        map.put('ı', 'i');
        map.put('ⅼ', 'l');
        map.put('０', '0');
        for (char c = '１'; c <= '９'; c++) {
            map.put(c, (char) ('1' + (c - '１')));
        }

        return map;
    }

    private static void putBoth(Map<Character, Character> map, char lower, char target) {
        map.put(lower, target);
        map.put(Character.toUpperCase(lower), target);
    }
}

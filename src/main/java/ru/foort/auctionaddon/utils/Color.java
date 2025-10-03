package ru.foort.auctionaddon.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Color {
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?:&#|#)([a-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)&x((&[a-f0-9]){6})");

    public static String translate(String message) {
        if (message == null || message.isEmpty()) return "";
        message = translateHex(message);
        message = translateLegacyHex(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String translateHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String translateLegacyHex(String message) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1).replace("&", "");
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String formatTime(String time) {
        if (time == null || time.isEmpty()) return "";

        String lower = time.toLowerCase();

        if (lower.contains("y") || lower.contains("year")) return time;
        if (lower.contains("mo") || lower.contains("month")) return time;
        if (lower.contains("w") || lower.contains("week")) return time;
        if (lower.contains("d") || lower.contains("day")) return time;
        if (lower.contains("h") || lower.contains("hour")) return time;
        if (lower.contains("m") || lower.contains("min")) return time;
        if (lower.contains("s") || lower.contains("sec") || lower.contains("second")) return time;

        return "";
    }
}

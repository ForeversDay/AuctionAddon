package ru.foort.auctionaddon.utils;

import ru.foort.auctionaddon.Main;
import java.util.*;

public class Utils {
    private static Main plugin;
    private static final Map<String, String> ruToEn = new HashMap<>();
    private static final List<String> ruNames = new ArrayList<>();

    public static long parseAmount(String input) {
        if (input == null) return -1;
        input = input.toLowerCase(Locale.ROOT).replace(" ", "").replace(",", "").replace(".", "");
        double multiplier = 1;
        if (input.endsWith("kk") || input.endsWith("кк")) { multiplier = 1_000_000; input = input.substring(0, input.length() - 2); }
        else if (input.endsWith("k") || input.endsWith("к")) { multiplier = 1_000; input = input.substring(0, input.length() - 1); }
        else if (input.endsWith("m") || input.endsWith("м")) { multiplier = 1_000_000; input = input.substring(0, input.length() - 1); }
        try { return (long) (Double.parseDouble(input) * multiplier); }
        catch (NumberFormatException e) { return -1; }
    }

    public static void loadTranslations(Main pl) {
        try {
            plugin = pl;
            java.io.File f = new java.io.File(plugin.getDataFolder(), "ru_ru.json");
            if (!f.exists()) return;
            String content = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            org.json.simple.JSONObject json = (org.json.simple.JSONObject) new org.json.simple.parser.JSONParser().parse(content);
            for (Object k : json.keySet()) {
                String en = String.valueOf(k).toLowerCase(Locale.ROOT);
                String ru = String.valueOf(json.get(k)).replace(" ", "_");
                ruToEn.put(ru.toLowerCase(Locale.ROOT), en);
                ruNames.add(ru);
            }
        } catch (Exception ignored) {}
    }

    public static List<String> getRuNames() {
        return ruNames;
    }

    public static Map<String, String> getRuToEn() {
        return ruToEn;
    }
}

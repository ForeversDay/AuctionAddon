package ru.foort.auctionaddon.utils;

import ru.foort.auctionaddon.Main;
import java.util.*;

public class Utils {
    private static Main plugin;
    private static final Map<String, String> ruToEn = new HashMap<>();
    private static final List<String> ruNames = new ArrayList<>();

    public static long parseAmount(String input) {
        if (input == null) return -1;
        input = input.toLowerCase(Locale.ROOT).replace(" ", "");
        input = input.replaceAll("[,']", "");
        input = input.replace(',', '.');
        double multiplier = 1;
        if (input.endsWith("kk") || input.endsWith("кк")) {
            multiplier = 1_000_000;
            input = input.substring(0, input.length() - 2);
        } else if (input.endsWith("k") || input.endsWith("к")) {
            multiplier = 1_000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m") || input.endsWith("м")) {
            multiplier = 1_000_000;
            input = input.substring(0, input.length() - 1);
        }
        try {
            double value = Double.parseDouble(input) * multiplier;
            if (value > Long.MAX_VALUE) return Long.MAX_VALUE;
            if (value < 0) return -1;
            return (long) value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void loadTranslations(Main pl) {
        try {
            plugin = pl;
            ruToEn.clear();
            ruNames.clear();

            java.io.File f = new java.io.File(plugin.getDataFolder(), "ru_ru.json");
            if (f.exists()) {
                String content = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                org.json.simple.JSONObject json = (org.json.simple.JSONObject)
                        new org.json.simple.parser.JSONParser().parse(content);

                for (Object k : json.keySet()) {
                    String en = String.valueOf(k).toLowerCase(Locale.ROOT);
                    String ru = String.valueOf(json.get(k)).toLowerCase(Locale.ROOT);
                    ruToEn.put(ru, en);
                    ruNames.add(ru);
                }
            }

            java.io.File itemsFile = new java.io.File(plugin.getDataFolder(), "data/items.yml");
            if (itemsFile.exists()) {
                org.bukkit.configuration.file.FileConfiguration cfg =
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(itemsFile);

                for (String key : cfg.getKeys(false)) {
                    String value = cfg.getString(key);
                    if (value == null) continue;
                    value = value.toLowerCase(Locale.ROOT);
                    ruToEn.put(value, key.toLowerCase(Locale.ROOT));
                    ruNames.add(value);
                }
            }

        } catch (Exception ignored) {
        }
    }

    public static List<String> getRuNames() {
        return ruNames;
    }

    public static Map<String, String> getRuToEn() {
        return ruToEn;
    }
}

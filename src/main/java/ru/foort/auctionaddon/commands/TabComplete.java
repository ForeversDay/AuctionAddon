package ru.foort.auctionaddon.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.utils.Utils;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {
    private static final DecimalFormat FORMATTER;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###", symbols);
    }

    private static BigInteger parseSuffix(String input) {
        if (input == null) return BigInteger.valueOf(-1);
        input = input.toLowerCase(Locale.ROOT).replace(" ", "");
        input = input.replace(',', '.');
        BigInteger multiplier = BigInteger.ONE;
        if (input.endsWith("kk") || input.endsWith("кк")) {
            multiplier = new BigInteger("1000000");
            input = input.substring(0, input.length() - 2);
        } else if (input.endsWith("k") || input.endsWith("к")) {
            multiplier = new BigInteger("1000");
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m") || input.endsWith("м")) {
            multiplier = new BigInteger("1000000");
            input = input.substring(0, input.length() - 1);
        }
        try {
            double value = Double.parseDouble(input) * multiplier.doubleValue();
            if (value < 0) return BigInteger.valueOf(-1);
            return BigInteger.valueOf((long) value);
        } catch (NumberFormatException e) {
            return BigInteger.valueOf(-1);
        }
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String lowerWords(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].toLowerCase(Locale.ROOT);
        }
        return String.join(" ", parts);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        if (args == null) args = new String[0];
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> baseCommands = Arrays.asList("help", "search", "sell", "dsell", "psell", "resell");
            for (String cmd : baseCommands) {
                if (cmd.startsWith(input)) completions.add(cmd);
            }

            if (!input.isEmpty()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String name = p.getName();
                    if (name.toLowerCase(Locale.ROOT).startsWith(input)) completions.add(name);
                }
            }
        } else if (args.length >= 2) {
            String subCommand = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);

            if (subCommand.equals("search")) {
                List<String> names = Utils.getRuNames() == null ? Collections.emptyList() : Utils.getRuNames();
                Set<String> added = new LinkedHashSet<>();
                String[] inputParts = Arrays.copyOfRange(args, 1, args.length);
                String lastPart = inputParts[inputParts.length - 1].toLowerCase(Locale.ROOT);

                for (String full : names) {
                    if (full == null) continue;
                    String fullNorm = full.replace("_", " ").trim();
                    String[] words = fullNorm.split("\\s+");

                    if (words.length < inputParts.length) continue;

                    boolean match = true;
                    for (int i = 0; i < inputParts.length - 1; i++) {
                        if (!words[i].toLowerCase(Locale.ROOT).equals(inputParts[i].toLowerCase(Locale.ROOT))) {
                            match = false;
                            break;
                        }
                    }
                    if (!match) continue;

                    String nextWords = String.join(" ", Arrays.copyOfRange(words, inputParts.length - 1, words.length));
                    if (nextWords.toLowerCase(Locale.ROOT).startsWith(lastPart)) {
                        String[] nextSplit = nextWords.split("\\s+", 2);
                        String first = capitalizeFirst(nextSplit[0]);
                        String rest = nextSplit.length > 1 ? lowerWords(nextSplit[1]) : "";
                        added.add((rest.isEmpty() ? first : first + " " + rest));
                    }
                }
                completions.addAll(added);
            } else if (subCommand.equals("sell") || subCommand.equals("dsell")) {
                String valStr = args[1] == null ? "" : args[1].trim().toLowerCase(Locale.ROOT);
                BigInteger val = parseSuffix(valStr);
                if (val.compareTo(BigInteger.ZERO) > 0) completions.add(FORMATTER.format(val));
            } else if (subCommand.equals("psell")) {
                String raw = args[1] == null ? "" : args[1].trim().toLowerCase(Locale.ROOT);
                if ("confirm".startsWith(raw)) completions.add("confirm");
                else {
                    BigInteger v = parseSuffix(raw);
                    if (v.compareTo(BigInteger.ZERO) > 0) completions.add(FORMATTER.format(v));
                    else completions.add("confirm");
                }
            }
        }

        return completions.stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
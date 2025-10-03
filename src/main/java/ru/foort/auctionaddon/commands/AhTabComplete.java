package ru.foort.auctionaddon.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.utils.Utils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AhTabComplete implements TabCompleter {
    private Utils utils;
    private static final DecimalFormat FORMATTER;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###", symbols);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("sell", "dsell", "search"));
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
        } else if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("search")) {
                String input = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
                for (String ruName : utils.getRuNames()) if (ruName.toLowerCase(Locale.ROOT).contains(input)) completions.add(ruName);
            } else if (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("dsell")) {
                try {
                    String clean = args[1].replace(",", "").replace(".", "");
                    long parsed = Long.parseLong(clean);
                    if (parsed > 0 && parsed <= Integer.MAX_VALUE) completions.add(FORMATTER.format(parsed));
                } catch (NumberFormatException ignored) {}
            }
        }
        return completions.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }
}

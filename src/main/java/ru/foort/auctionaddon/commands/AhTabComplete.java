package ru.foort.auctionaddon.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.utils.Utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class AhTabComplete implements TabCompleter {
    private final Utils utils;
    private static final DecimalFormat FORMATTER;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###", symbols);
    }

    public AhTabComplete(Utils utils) {
        this.utils = utils;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) args = new String[0];
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0] == null ? "" : args[0].trim().toLowerCase(Locale.ROOT);
            List<String> baseCommands = new ArrayList<>(Arrays.asList("{player}", "dsell", "help", "search", "sell"));
            if (input.isEmpty()) {
                completions.addAll(baseCommands);
            } else {
                for (String cmd : baseCommands) {
                    if (cmd.startsWith(input)) completions.add(cmd);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String name = p.getName();
                    if (name.toLowerCase(Locale.ROOT).startsWith(input)) completions.add(name);
                }
            }
        } else if (args.length >= 2) {
            if (args[0] != null && args[0].equalsIgnoreCase("search")) {
                String input = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
                for (String ruName : utils.getRuNames()) {
                    if (ruName.toLowerCase(Locale.ROOT).contains(input)) completions.add(ruName);
                }
            } else if (args[0] != null && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("dsell"))) {
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
package ru.foort.auctionaddon.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.Main;
import ru.foort.auctionaddon.commands.impl.AddItemsCommand;
import ru.foort.auctionaddon.commands.impl.PSellCommand;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import ru.foort.auctionaddon.commands.impl.SellCommand;
import ru.foort.auctionaddon.commands.impl.ResellCommand;
import ru.foort.auctionaddon.commands.impl.SearchCommand;
import ru.foort.auctionaddon.commands.impl.ViewCommand;
import org.by1337.bauction.bmenu.menu.MenuLoader;

public class Command implements CommandExecutor {
    private final Main plugin;
    private final MenuLoader menuLoader;
    private final String homeMenuId;
    private final String viewMenuId;
    private final SellCommand sellCommand;
    private final PSellCommand pSellCommand;
    private final ResellCommand resellCommand;
    private final SearchCommand searchCommand;
    private final ViewCommand viewCommand;
    private AddItemsCommand addItemsCommand;

    public Command(Main plugin, MenuLoader menuLoader, String homeMenuId, String viewMenuId) {
        this.plugin = plugin;
        this.menuLoader = menuLoader;
        this.homeMenuId = homeMenuId;
        this.viewMenuId = viewMenuId;
        this.sellCommand = new SellCommand(plugin);
        this.pSellCommand = new PSellCommand(plugin);
        this.resellCommand = new ResellCommand(plugin);
        this.searchCommand = new SearchCommand(plugin, menuLoader, viewMenuId);
        this.viewCommand = new ViewCommand(plugin, menuLoader, viewMenuId);
        this.addItemsCommand = new AddItemsCommand(plugin);
        Utils.loadTranslations(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cИспользовать можно только от игрока!"));
            return true;
        }
        if (args.length == 0) {
            var menu = menuLoader.getMenu(homeMenuId);
            if (menu != null) menu.create(player, null).open();
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help", "{player}" -> {
                for (String line : plugin.getConfig().getStringList("messages.help"))
                    player.sendMessage(Color.translate(line));
                return true;
            }
            case "author", "authors" -> {
                player.sendMessage(Color.translate("&eauthor: &fFoort"));
                return true;
            }
            case "sell" -> {
                return sellCommand.onCommand(sender, command, label, args);
            }
            case "psell" -> {
                return pSellCommand.onCommand(sender, command, label, args);
            }
            case "resell" -> {
                return resellCommand.onCommand(sender, command, label, args);
            }
            case "search" -> {
                return new SearchCommand(plugin, menuLoader, homeMenuId).onCommand(sender, command, label, args);
            }
            case "additems" -> {
                return addItemsCommand.onCommand(sender, command, label, args);
            }
            default -> {
                return viewCommand.onCommand(sender, command, label, args);
            }
        }
    }
}
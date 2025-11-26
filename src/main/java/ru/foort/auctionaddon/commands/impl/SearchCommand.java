package ru.foort.auctionaddon.commands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.by1337.bauction.Main;
import org.by1337.bauction.bmenu.menu.MenuLoader;
import org.by1337.bauction.menu.HomeMenu;
import org.by1337.bauction.util.auction.Category;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import java.util.*;
import java.util.stream.Collectors;

public class SearchCommand implements CommandExecutor {
    private final ru.foort.auctionaddon.Main plugin;
    private final MenuLoader menuLoader;
    private final String homeMenuId;

    public SearchCommand(ru.foort.auctionaddon.Main plugin, MenuLoader menuLoader, String homeMenuId) {
        this.plugin = plugin;
        this.menuLoader = menuLoader;
        this.homeMenuId = homeMenuId;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cВася, зайди в игру, а не от консоли пиши команды!"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.search_usage")));
            return true;
        }

        String input = String.join("_", Arrays.copyOfRange(args, 1, args.length)).trim().toLowerCase(Locale.ROOT);
        Map<String, String> map = Utils.getRuToEn();
        if (map == null || map.isEmpty()) {
            player.sendMessage(Color.translate("&cТаблица переводов пуста!"));
            return true;
        }

        Set<String> resultIDs = map.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(input))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        if (resultIDs.isEmpty()) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.search_no_item")));
            return true;
        }

        Category custom = Main.getCfg().getSorting().getAs("special.search", Category.class);
        custom.setSoft(true);
        custom.setTags(resultIDs);

        var menu = menuLoader.getMenu(homeMenuId);
        if (menu == null) {
            plugin.getLogger().severe(Color.translate("&cMenu: home не найдено!"));
            return true;
        }

        var m = menu.create(player, null);
        if (m instanceof HomeMenu homeMenu) {
            homeMenu.setCustom(custom);
            homeMenu.getCategories().add(custom);
            Collections.sort(homeMenu.getCategories());
        }

        m.open();
        return true;
    }
}
package ru.foort.auctionaddon.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.by1337.bauction.Main;
import org.by1337.bauction.db.kernel.User;
import org.by1337.bauction.lang.Lang;
import org.by1337.bauction.menu.HomeMenu;
import org.by1337.bauction.menu.PlayerItemsView;
import org.by1337.bauction.util.auction.Category;
import org.by1337.bauction.bmenu.menu.MenuLoader;
import ru.foort.auctionaddon.dsell.DSellEvent;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class AhCommand implements CommandExecutor {
    private final ru.foort.auctionaddon.Main plugin;
    private final MenuLoader menuLoader;
    private final String homeMenuId;
    private final String viewMenuId;
    private Utils utils;

    public AhCommand(ru.foort.auctionaddon.Main plugin, MenuLoader menuLoader, String homeMenuId, String viewMenuId) {
        this.plugin = plugin;
        this.menuLoader = menuLoader;
        this.homeMenuId = homeMenuId;
        this.viewMenuId = viewMenuId;
        utils.loadTranslations();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player))
            return true;
        if (args.length == 0) {
            var menu = menuLoader.getMenu(homeMenuId);
            if (menu != null) menu.create(player, null).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                int sec = plugin.getConfig().getInt("settings.dsell_seconds", 10);
                for (String line : plugin.getConfig().getStringList("messages.help"))
                    player.sendMessage(Color.translate(line.replace("%sec%", String.valueOf(sec))));
                return true;
            }
            case "author", "authors" -> {
                for (String line : plugin.getConfig().getStringList("messages.author"))
                    player.sendMessage(Color.translate(line));
                return true;
            }
            case "sell", "dsell" -> {
                if (args.length < 2) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString(sub + "_usage")));
                    return true;
                }
                long priceL = utils.parseAmount(args[1]);
                if (priceL < plugin.getConfig().getInt("settings.sell_min-price", 10)) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.min_price").replace("%min%", String.valueOf(plugin.getConfig().getInt("settings.sell_min-price", 10)))));
                    return true;
                }
                if (priceL > plugin.getConfig().getInt("settings.sell_max-price", 100000000)) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.max_price").replace("%max%", String.valueOf(plugin.getConfig().getInt("settings.sell_max-price", 100000000)))));
                    return true;
                }
                int price = (int) priceL;
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_item")));
                    return true;
                }
                if (Main.getBlackList().stream().anyMatch(tag -> hand.getType().name().equalsIgnoreCase(tag))) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.item_in_black_list")));
                    return true;
                }
                if (sub.equals("sell")) {
                    try {
                        User user = Main.getStorage().getUserOrCreate(player);
                        boolean saleByThePiece = Main.getCfg().isAllowBuyCount();
                        var sellItem = new org.by1337.bauction.db.kernel.SellItem(player, hand.clone(), price, Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(), saleByThePiece);
                        for (String tag : sellItem.getTags()) {
                            if (Main.getBlackList().contains(tag)) {
                                Main.getMessage().sendMsg(player, sellItem.replace(Lang.getMessage("item_in_black_list")));
                                player.getInventory().addItem(hand);
                                return true;
                            }
                        }
                        var event = new org.by1337.bauction.db.event.SellItemEvent(user, sellItem);
                        Main.getStorage().validateAndAddItem(event);
                        if (event.isValid()) {
                            Main.getEventManager().onEvent(new org.by1337.bauction.event.Event(player, org.by1337.bauction.event.EventType.SELL_ITEM, null));
                            String listedMsg = plugin.getConfig().getString("messages.sell_item", "&e[⚝] &f%player% выставил [x%count% %item%] за $%price%");
                            listedMsg = listedMsg.replace("%player%", player.getName())
                                    .replace("%item%", hand.getType().name())
                                    .replace("%count%", String.valueOf(hand.getAmount()))
                                    .replace("%price%", String.valueOf(price));
                            player.sendMessage(Color.translate(listedMsg));
                        } else {
                            player.getInventory().addItem(hand);
                        }
                    } catch (Exception ex) {
                        player.getInventory().addItem(hand);
                        ex.printStackTrace();
                    }
                } else {
                    int delay = plugin.getConfig().getInt("settings.dsell_seconds", 10);
                    Bukkit.getPluginManager().callEvent(new DSellEvent(player, hand.clone(), price, delay));
                }
                player.getInventory().setItemInMainHand(null);
                return true;
            }
            case "search" -> {
                if (args.length < 2) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.search_usage")));
                    return true;
                }
                String input = String.join("_", Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
                Set<String> resultTags = utils.getRuToEn().keySet().stream().filter(tag -> tag.contains(input)).collect(Collectors.toSet());
                if (resultTags.isEmpty()) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.search_no_item")));
                    return true;
                }
                Category custom = Main.getCfg().getSorting().getAs("special.search", Category.class);
                custom.setSoft(true);
                custom.setTags(resultTags.stream().map(utils.getRuToEn()::get).collect(Collectors.toSet()));
                var menu = menuLoader.getMenu(homeMenuId);
                if (menu == null) {
                    player.sendMessage(Color.translate("&fЭх, опять… То ли я что-то намудрил, то ли ты забыл в Main поменять название меню на своё."));
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
            default -> {
                UUID uuid;
                Player target = Bukkit.getPlayerExact(sub);
                if (target != null) {
                    uuid = target.getUniqueId();
                } else {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(sub);
                    uuid = offline.getUniqueId();
                }
                User user = Main.getStorage().getUser(uuid);
                if (user == null) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_player")));
                    return true;
                }
                var menu = menuLoader.getMenu(viewMenuId);
                if (menu == null) {
                    player.sendMessage(Color.translate("&fЭх, опять… То ли я что-то намудрил, то ли ты забыл в Main поменять название меню на своё."));
                    return true;
                }
                var m = menu.create(player, null);
                if (m instanceof PlayerItemsView view) {
                    view.setUuid(uuid);
                    view.setName(user.getNickName());
                }
                m.open();
                return true;
            }
        }
    }
}
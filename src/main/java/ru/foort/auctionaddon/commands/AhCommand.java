package ru.foort.auctionaddon.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

import java.util.*;
import java.util.stream.Collectors;

public class AhCommand implements CommandExecutor {
    private final ru.foort.auctionaddon.Main plugin;
    private final MenuLoader menuLoader;
    private final String homeMenuId;
    private final String viewMenuId;
    private final Map<String, Integer> maxItemPrices = new HashMap<>();

    public AhCommand(ru.foort.auctionaddon.Main plugin, MenuLoader menuLoader, String homeMenuId, String viewMenuId) {
        this.plugin = plugin;
        this.menuLoader = menuLoader;
        this.homeMenuId = homeMenuId;
        this.viewMenuId = viewMenuId;
        Utils.loadTranslations(plugin);
        loadMaxItemPrices();
    }

    private void loadMaxItemPrices() {
        maxItemPrices.clear();
        for (String entry : plugin.getConfig().getStringList("max_price_items")) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                try {
                    String material = parts[0].toUpperCase(Locale.ROOT);
                    int price = Integer.parseInt(parts[1]);
                    maxItemPrices.put(material, price);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
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
                long priceL = Utils.parseAmount(args[1]);
                int minPrice = plugin.getConfig().getInt("settings.sell_min-price", 10);
                if (priceL < minPrice) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.min_price").replace("%min%", String.valueOf(minPrice))));
                    return true;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_item")));
                    return true;
                }
                String materialName = hand.getType().name().toUpperCase(Locale.ROOT);
                int globalMax = plugin.getConfig().getInt("settings.sell_max-price", 100000000);
                int itemMax = maxItemPrices.getOrDefault(materialName, globalMax);
                if (priceL > itemMax) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.max_price").replace("%max%", String.valueOf(itemMax))));
                    return true;
                }
                int price = (int) priceL;
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
                Set<String> resultTags = Utils.getRuToEn().keySet().stream().filter(tag -> tag.contains(input)).collect(Collectors.toSet());
                if (resultTags.isEmpty()) {
                    player.sendMessage(Color.translate(plugin.getConfig().getString("messages.search_no_item")));
                    return true;
                }
                Category custom = Main.getCfg().getSorting().getAs("special.search", Category.class);
                custom.setSoft(true);
                custom.setTags(resultTags.stream().map(Utils.getRuToEn()::get).collect(Collectors.toSet()));
                var menu = menuLoader.getMenu(homeMenuId);
                if (menu == null) {
                    plugin.getInstance().getLogger().severe(Color.translate("&cMenu: home не найдено!"));
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
                    plugin.getInstance().getLogger().severe(Color.translate("&cMenu: view не найдено!"));
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
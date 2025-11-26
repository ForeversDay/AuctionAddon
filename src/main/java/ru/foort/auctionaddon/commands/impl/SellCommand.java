package ru.foort.auctionaddon.commands.impl;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.ShulkerBox;
import ru.foort.auctionaddon.Main;
import org.by1337.bauction.db.event.SellItemEvent;
import org.by1337.bauction.db.kernel.SellItem;
import org.by1337.bauction.db.kernel.User;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import java.util.List;

public class SellCommand implements CommandExecutor {
    private final Main plugin;

    public SellCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cВася, зайди в игру, а не от консоли пиши команды!"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.sell_usage")));
            return true;
        }
        long priceL = Utils.parseAmount(args[1]);
        if (priceL == -1) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.invalid_price")));
            return true;
        }
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
        long globalMax = plugin.getConfig().getInt("settings.sell_max-price", 100000000);
        long itemMax = globalMax;

        try {
            List<String> maxPriceItems = plugin.getConfig().getStringList("max_price_items");
            for (String entry : maxPriceItems) {
                String[] parts = entry.split(":");
                if (parts.length == 2 && hand.getType().name().equalsIgnoreCase(parts[0])) {
                    itemMax = Integer.parseInt(parts[1]);
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        long price = (long) priceL;
        if (price > itemMax) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.max_price").replace("%max%", String.valueOf(itemMax))));
            return true;
        }

        if (org.by1337.bauction.Main.getBlackList().stream().anyMatch(tag -> hand.getType().name().equalsIgnoreCase(tag))) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.item_in_black_list")));
            return true;
        }

        boolean allowShulkerWithItems = plugin.getConfig().getBoolean("settings.allow_shulker_with_items", false);

        if (hand.getType().name().contains("SHULKER_BOX")) {
            if (hand.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta meta) {
                if (meta.getBlockState() instanceof org.bukkit.block.ShulkerBox box) {
                    boolean filled = false;
                    for (ItemStack content : box.getInventory().getContents()) {
                        if (content != null && !content.getType().isAir()) {
                            filled = true;
                            break;
                        }
                    }
                    if (filled && !allowShulkerWithItems) {
                        player.sendMessage(Color.translate(plugin.getConfig().getString("messages.shulker_not_allowed")));
                        return true;
                    }
                }
            }
        }

        try {
            User user = org.by1337.bauction.Main.getStorage().getUserOrCreate(player);
            boolean saleByThePiece = org.by1337.bauction.Main.getCfg().isAllowBuyCount();
            SellItem sellItem = new SellItem(player, hand.clone(), price, org.by1337.bauction.Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(), saleByThePiece);

            for (String tag : sellItem.getTags()) {
                if (org.by1337.bauction.Main.getBlackList().contains(tag)) {
                    player.sendMessage(Color.translate(org.by1337.bauction.lang.Lang.getMessage("item_in_black_list")));
                    player.getInventory().addItem(hand);
                    return true;
                }
            }

            SellItemEvent event = new SellItemEvent(user, sellItem);
            org.by1337.bauction.Main.getStorage().validateAndAddItem(event);

            if (event.isValid()) {
                String listedMsg = plugin.getConfig().getString("messages.sell_item", "&e[⚝] &f%player% выставил [x%count% %item%] за $%price%");
                listedMsg = listedMsg.replace("%player%", player.getName())
                        .replace("%item%", hand.getType().name())
                        .replace("%count%", String.valueOf(hand.getAmount()))
                        .replace("%price%", String.valueOf(price));
                player.sendMessage(Color.translate(listedMsg));
                playSound(player, "sell");
            } else {
                player.getInventory().addItem(hand);
            }
        } catch (Exception ex) {
            player.getInventory().addItem(hand);
            ex.printStackTrace();
        }

        player.getInventory().setItemInMainHand(null);
        return true;
    }

    private void playSound(Player player, String path) {
        String soundName = plugin.getConfig().getString("settings.sounds." + path, "NONE");
        if (soundName != null && !soundName.equalsIgnoreCase("NONE")) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1f, 1f);
            } catch (Exception ignored) {
            }
        }
    }
}
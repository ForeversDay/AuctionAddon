package ru.foort.auctionaddon.commands.impl;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import ru.foort.auctionaddon.Main;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import org.by1337.bauction.db.event.SellItemEvent;
import org.by1337.bauction.db.kernel.SellItem;
import org.by1337.bauction.db.kernel.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PSellCommand implements CommandExecutor {
    private final Main plugin;
    private final Map<UUID, SellItem> pendingPublicSell = new HashMap<>();

    public PSellCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cТолько игрок может использовать эту команду!"));
            return true;
        }
        if (args == null || args.length < 2) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.public_usage")));
            return true;
        }
        String second = args[1];
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (second.equalsIgnoreCase("confirm")) {
            SellItem item = pendingPublicSell.remove(player.getUniqueId());
            if (item == null || hand == null || hand.getType().isAir() || !hand.isSimilar(item.getItemStack())) {
                player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_pending_public")));
                return true;
            }
            if (!checkShulkerAllowed(player, hand)) return true;
            player.getInventory().setItemInMainHand(null);
            try {
                User user = org.by1337.bauction.Main.getStorage().getUserOrCreate(player);
                SellItemEvent event = new SellItemEvent(user, item);
                org.by1337.bauction.Main.getStorage().validateAndAddItem(event);
                if (event.isValid()) {
                    List<String> broadcast = plugin.getConfig().getStringList("messages.public_broadcast");
                    String openCommandTemplate = plugin.getConfig().getString("open_seller_auction.command", "ah {seller}");
                    String openText = plugin.getConfig().getString("open_seller_auction.text", "&a[Посмотреть аукцион игрока]");
                    String openHover = plugin.getConfig().getString("open_seller_auction.hoverMessage", "&fНажми, чтобы открыть");
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        for (String line : broadcast) {
                            if (line.contains("{open_seller_auction}") || line.contains("%open_seller_auction%")) {
                                String prefix = line.replace("{open_seller_auction}", "").replace("%open_seller_auction%", "");
                                prefix = applyPlaceholders(prefix, player, item);
                                TextComponent msg = new TextComponent(Color.translate(prefix));
                                String command = openCommandTemplate.replace("{seller}", player.getName()).replace("%seller%", player.getName());
                                TextComponent clickable = new TextComponent(Color.translate(openText));
                                clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command));
                                clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Color.translate(openHover)).create()));
                                msg.addExtra(" ");
                                msg.addExtra(clickable);
                                online.spigot().sendMessage(msg);
                            } else {
                                online.sendMessage(Color.translate(applyPlaceholders(line, player, item)));
                            }
                        }
                    }
                    playSound(player, "sell");
                } else {
                    player.getInventory().addItem(item.getItemStack());
                }
            } catch (Exception ex) {
                player.getInventory().addItem(item.getItemStack());
                ex.printStackTrace();
            }
            return true;
        }

        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_item")));
            return true;
        }

        if (!checkShulkerAllowed(player, hand)) return true;

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

        double feePercent = plugin.getConfig().getDouble("settings.public_fee_percent", 10.0);
        double feeAmount = price * feePercent / 100.0;

        Economy econ = plugin.getEconomy();
        if (econ.getBalance(player) < feeAmount) {
            String msg = plugin.getConfig().getString("messages.not_enough_money_public", "&cУ вас недостаточно денег для публичной продажи!");
            player.sendMessage(Color.translate(msg.replace("%fee%", String.valueOf((int) feeAmount))));
            return true;
        }

        EconomyResponse response = econ.withdrawPlayer(player, feeAmount);
        if (response == null || !response.transactionSuccess()) {
            String msg = plugin.getConfig().getString("messages.not_enough_money_public", "&cУ вас недостаточно денег для публичной продажи!");
            player.sendMessage(Color.translate(msg.replace("%fee%", String.valueOf((int) feeAmount))));
            return true;
        }

        try {
            User user = org.by1337.bauction.Main.getStorage().getUserOrCreate(player);
            boolean saleByThePiece = org.by1337.bauction.Main.getCfg().isAllowBuyCount();
            SellItem sellItem = new SellItem(player, hand.clone(), price, org.by1337.bauction.Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(), saleByThePiece);
            pendingPublicSell.put(player.getUniqueId(), sellItem);
        } catch (Exception ex) {
            ex.printStackTrace();
            player.getInventory().addItem(hand);
            return true;
        }

        for (String msg : plugin.getConfig().getStringList("messages.public_preview")) {
            msg = msg.replace("%price_broad%", String.valueOf((int) feeAmount))
                    .replace("%price_items%", String.valueOf(price));
            player.sendMessage(Color.translate(msg));
        }

        player.sendMessage(Color.translate(plugin.getConfig().getString("messages.public_need_confirm")));
        return true;
    }

    private boolean checkShulkerAllowed(Player player, ItemStack hand) {
        boolean allowShulker = plugin.getConfig().getBoolean("settings.allow_shulker_with_items", false);
        if (hand.getType().name().contains("SHULKER_BOX")) {
            if (hand.getItemMeta() instanceof BlockStateMeta meta) {
                if (meta.getBlockState() instanceof ShulkerBox box) {
                    for (ItemStack content : box.getInventory().getContents()) {
                        if (content != null && !content.getType().isAir() && !allowShulker) {
                            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.shulker_not_allowed")));
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private String applyPlaceholders(String line, Player seller, SellItem item) {
        String itemName = formatItemName(item.getItemStack());
        int amount = item.getItemStack().getAmount();
        int price = (int) item.getPrice();
        int priceOne = price / Math.max(1, amount);
        line = line.replace("%seller%", seller.getName())
                .replace("%items%", itemName.toUpperCase().replace(' ', '_'))
                .replace("%amount%", String.valueOf(amount))
                .replace("%price_items%", String.valueOf(price))
                .replace("%price_one_items%", String.valueOf(priceOne));
        return line;
    }

    private String formatItemName(ItemStack item) {
        if (item == null) return "AIR";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String name = item.getType().name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private void playSound(Player player, String path) {
        String soundName = plugin.getConfig().getString("settings.sounds." + path, "NONE");
        if (soundName != null && !soundName.equalsIgnoreCase("NONE")) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1f, 1f);
            } catch (Exception ignored) {}
        }
    }
}
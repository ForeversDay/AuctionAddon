package ru.foort.auctionaddon.dsell;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import ru.foort.auctionaddon.Main;
import org.by1337.bauction.db.kernel.SellItem;
import org.by1337.bauction.db.kernel.User;
import org.by1337.bauction.db.event.SellItemEvent;
import org.by1337.bauction.lang.Lang;
import ru.foort.auctionaddon.utils.Color;
import java.util.Map;

public class DSellListener implements Listener {

    @EventHandler
    public void onDSellEvent(DSellEvent e) {
        Player owner = e.getPlayer();
        ItemStack item = e.getItem().clone();
        int price = e.getPrice();
        int delay = e.getDelay();

        String startMsg = Main.getInstance().getConfig().getString("messages.dsell_start",
                "&e[⚝] &fВаш предмет будет выставлен на аукцион через &#FFE000%sec% сек.");
        startMsg = startMsg.replace("%sec%", String.valueOf(delay))
                .replace("%item%", item.getType().name())
                .replace("%player%", owner.getName())
                .replace("%count%", String.valueOf(item.getAmount()))
                .replace("%price%", String.valueOf(price));
        owner.sendMessage(Color.translate(startMsg));

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Player p = Bukkit.getPlayerExact(owner.getName());
            if (p == null || !p.isOnline()) {
                Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                Bukkit.getWorlds().get(0).dropItemNaturally(spawn, item);
                return;
            }
            try {
                User user = org.by1337.bauction.Main.getStorage().getUserOrCreate(p);
                boolean saleByThePiece = org.by1337.bauction.Main.getCfg().isAllowBuyCount();
                SellItem sellItem = new SellItem(p, item, price,
                        org.by1337.bauction.Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(),
                        saleByThePiece);

                for (String tag : sellItem.getTags()) {
                    if (org.by1337.bauction.Main.getBlackList().contains(tag)) {
                        org.by1337.bauction.Main.getMessage().sendMsg(p, sellItem.replace(Lang.getMessage("item_in_black_list")));
                        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
                        if (!left.isEmpty()) for (ItemStack leftItem : left.values())
                            p.getWorld().dropItemNaturally(p.getLocation(), leftItem);
                        return;
                    }
                }

                SellItemEvent event = new SellItemEvent(user, sellItem);
                org.by1337.bauction.Main.getStorage().validateAndAddItem(event);

                if (event.isValid()) {
                    org.by1337.bauction.Main.getEventManager().onEvent(
                            new org.by1337.bauction.event.Event(p, org.by1337.bauction.event.EventType.SELL_ITEM, null)
                    );

                    String listedMsg = Main.getInstance().getConfig().getString("messages.sell_item",
                            "&e[⚝] &f%player% выставил [&#FFE000x%count% %item%] за &#FFE000$%price%");
                    listedMsg = listedMsg
                            .replace("%player%", p.getName())
                            .replace("%item%", item.getType().name())
                            .replace("%count%", String.valueOf(item.getAmount()))
                            .replace("%price%", String.valueOf(price));
                    p.sendMessage(Color.translate(listedMsg));
                } else {
                    String reason = String.valueOf(event.getReason());
                    try { org.by1337.bauction.Main.getMessage().sendMsg(p, reason); } catch (Throwable ignored) {}
                    Map<Integer, ItemStack> left = p.getInventory().addItem(item);
                    if (!left.isEmpty()) for (ItemStack leftItem : left.values())
                        p.getWorld().dropItemNaturally(p.getLocation(), leftItem);
                }
            } catch (Throwable t) {
                Map<Integer, ItemStack> left = p.getInventory().addItem(item);
                if (!left.isEmpty()) for (ItemStack leftItem : left.values())
                    p.getWorld().dropItemNaturally(p.getLocation(), leftItem);
            }
        }, delay * 20L);
    }
}
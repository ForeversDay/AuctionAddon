package ru.foort.auctionaddon.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.foort.auctionaddon.Main;
import org.by1337.bauction.api.event.BuyItemProcess;
import org.by1337.bauction.api.event.BuyItemCountProcess;
import org.by1337.bauction.db.kernel.SellItem;
import org.by1337.bauction.db.kernel.User;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BuyEvent implements Listener {
    private final Main plugin;

    public BuyEvent(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFullPurchase(BuyItemProcess event) {
        notifySeller(event.getSellItem(), event.getBuyer(), event.getSellItem().getItemStack().getAmount());
    }

    @EventHandler
    public void onPartialPurchase(BuyItemCountProcess event) {
        notifySeller(event.getItem(), event.getBuyer(), event.getCount());
    }


    private void notifySeller(SellItem item, User buyer, int count) {
        String buyerName = buyer.getNickName();
        String itemName = item.getItemStack().getType().name();
        int price = (int) ((item.getPrice() * count) / item.getItemStack().getAmount());
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String msg = plugin.getConfig().getString("messages.sale_offline")
                .replace("%buyer%", buyerName)
                .replace("%count%", String.valueOf(count))
                .replace("%item%", itemName)
                .replace("%price%", String.valueOf(price))
                .replace("%time%", time)
                .replace("%date%", date);
        saveOfflineMessage(item.getSellerUuid().toString(), msg);
    }

    private void saveOfflineMessage(String uuid, String message) {
        try {
            File dir = new File(plugin.getDataFolder(), "data");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, uuid + ".txt");
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(message + "\n");
            }
        } catch (Exception ignored) {
        }
    }
}

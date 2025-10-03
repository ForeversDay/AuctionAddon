package ru.foort.auctionaddon.dsell;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class DSellEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ItemStack item;
    private final int price;
    private final int delay;

    public DSellEvent(Player player, ItemStack item, int price, int delay) {
        this.player = player;
        this.item = item;
        this.price = price;
        this.delay = delay;
    }

    public Player getPlayer() { return player; }
    public ItemStack getItem() { return item; }
    public int getPrice() { return price; }
    public int getDelay() { return delay; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

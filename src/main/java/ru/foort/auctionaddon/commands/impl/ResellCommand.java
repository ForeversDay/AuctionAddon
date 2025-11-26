package ru.foort.auctionaddon.commands.impl;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.foort.auctionaddon.Main;
import ru.foort.auctionaddon.utils.Color;
import org.by1337.bauction.db.kernel.SellItem;
import org.by1337.bauction.db.kernel.UnsoldItem;
import org.by1337.bauction.db.kernel.User;
import org.by1337.bauction.db.event.SellItemEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ResellCommand implements CommandExecutor {
    private final Main plugin;
    private final Map<UUID, Long> resellCooldowns = new ConcurrentHashMap<>();

    public ResellCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cВася, зайди в игру, а не от консоли пиши команды!"));
            return true;
        }
        long cooldown = plugin.getConfig().getLong("settings.resell_cooldown", 60);
        long now = System.currentTimeMillis();
        if (resellCooldowns.containsKey(player.getUniqueId())) {
            long end = resellCooldowns.get(player.getUniqueId());
            long remaining = (end - now) / 1000;
            if (remaining > 0) {
                player.sendMessage(Color.translate(plugin.getConfig().getString("messages.resell_cooldown").replace("%time%", String.valueOf(remaining))));
                playSound(player, "resell_cooldown");
                return true;
            }
        }
        try {
            User user = org.by1337.bauction.Main.getStorage().getUserOrCreate(player);
            List<SellItem> sellItems = new ArrayList<>();
            List<UnsoldItem> unsoldItems = new ArrayList<>();
            org.by1337.bauction.Main.getStorage().forEachSellItemsByUser(sellItems::add, user.getUuid());
            org.by1337.bauction.Main.getStorage().forEachUnsoldItemsByUser(unsoldItems::add, user.getUuid());
            if (sellItems.isEmpty() && unsoldItems.isEmpty()) {
                player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_items_resell")));
                return true;
            }
            AtomicInteger removedCount = new AtomicInteger();
            AtomicInteger relistedCount = new AtomicInteger();
            Object storage = org.by1337.bauction.Main.getStorage();
            Method removeSellMethod = findSingleParamMethod(storage.getClass(), "removeSellItem");
            Method removeUnsoldMethod = findSingleParamMethod(storage.getClass(), "removeUnsoldItem");

            for (SellItem s : sellItems) {
                try {
                    Object unique = extractUniqueArgForRemove(storage, removeSellMethod, s);
                    if (removeSellMethod != null && unique != null) {
                        removeSellMethod.invoke(storage, unique);
                        removedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            }
            for (UnsoldItem u : unsoldItems) {
                try {
                    Object unique = extractUniqueArgForRemove(storage, removeUnsoldMethod, u);
                    if (removeUnsoldMethod != null && unique != null) {
                        removeUnsoldMethod.invoke(storage, unique);
                        removedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            }
            for (SellItem s : sellItems) {
                try {
                    ItemStack clone = safeCloneItemStack(s);
                    int amount = safeGetInt(s, "getAmount");
                    if (amount > 0) clone.setAmount(amount);
                    double priceD = safeGetNumberAsDouble(s, "getPrice", "getPriceDouble", "getCost");
                    int price = (int) Math.round(priceD);
                    boolean saleByThePiece = safeGetBoolean(s, "isSaleByThePiece", "getSaleByThePiece");
                    SellItem newItem = new SellItem(player, clone, price, org.by1337.bauction.Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(), saleByThePiece);
                    boolean black = false;
                    for (String tag : newItem.getTags()) {
                        if (org.by1337.bauction.Main.getBlackList().contains(tag)) {
                            black = true;
                            break;
                        }
                    }
                    if (black) continue;
                    SellItemEvent event = new SellItemEvent(user, newItem);
                    org.by1337.bauction.Main.getStorage().validateAndAddItem(event);
                    if (event.isValid()) relistedCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }
            for (UnsoldItem u : unsoldItems) {
                try {
                    ItemStack clone = safeCloneItemStack(u);
                    int amount = safeGetInt(u, "getAmount");
                    if (amount > 0) clone.setAmount(amount);
                    double priceD = safeGetNumberAsDouble(u, "getPrice", "getSellPrice", "getOldPrice");
                    int price = (int) Math.round(priceD);
                    boolean saleByThePiece = org.by1337.bauction.Main.getCfg().isAllowBuyCount();
                    SellItem newItem = new SellItem(player, clone, price, org.by1337.bauction.Main.getCfg().getDefaultSellTime() + user.getExternalSellTime(), saleByThePiece);
                    boolean black = false;
                    for (String tag : newItem.getTags()) {
                        if (org.by1337.bauction.Main.getBlackList().contains(tag)) {
                            black = true;
                            break;
                        }
                    }
                    if (black) continue;
                    SellItemEvent event = new SellItemEvent(user, newItem);
                    org.by1337.bauction.Main.getStorage().validateAndAddItem(event);
                    if (event.isValid()) relistedCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.resell_success")));
            playSound(player, "resell");
            resellCooldowns.put(player.getUniqueId(), now + cooldown * 1000);
        } catch (Exception ex) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.resell_error", "&cОшибка при повторном выставлении предметов.")));
        }
        return true;
    }

    private static Method findSingleParamMethod(Class<?> cls, String name) {
        for (Method m : cls.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == 1) return m;
        return null;
    }

    private static Object extractUniqueArgForRemove(Object storage, Method removeMethod, Object item) {
        if (removeMethod == null) return null;
        Class<?> param = removeMethod.getParameterTypes()[0];
        for (Method m : item.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && param.isAssignableFrom(m.getReturnType())) {
                try {
                    return m.invoke(item);
                } catch (Exception ignored) {
                }
            }
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            if (param.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    return f.get(item);
                } catch (Exception ignored) {
                }
            }
        }
        String[] candidates = {"getUniqueName", "getUnique", "getUniqueId", "getUuid", "getId", "getName"};
        for (String name : candidates) {
            try {
                Method m = item.getClass().getMethod(name);
                if (m.getParameterCount() == 0 && param.isAssignableFrom(m.getReturnType())) return m.invoke(item);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static org.bukkit.inventory.ItemStack safeCloneItemStack(Object item) {
        try {
            Method m = item.getClass().getMethod("getItemStack");
            Object o = m.invoke(item);
            if (o instanceof org.bukkit.inventory.ItemStack is) return is.clone();
        } catch (Exception ignored) {
        }
        try {
            Method m = item.getClass().getMethod("getItem");
            Object o = m.invoke(item);
            if (o instanceof org.bukkit.inventory.ItemStack is) return is.clone();
        } catch (Exception ignored) {
        }
        return new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE, 1);
    }

    private static int safeGetInt(Object item, String... names) {
        for (String name : names) {
            try {
                Method m = item.getClass().getMethod(name);
                Object o = m.invoke(item);
                if (o instanceof Number n) return n.intValue();
            } catch (Exception ignored) {
            }
        }
        try {
            Method m = item.getClass().getMethod("getItemStack");
            Object o = m.invoke(item);
            if (o instanceof org.bukkit.inventory.ItemStack is) return is.getAmount();
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static double safeGetNumberAsDouble(Object item, String... names) {
        for (String name : names) {
            try {
                Method m = item.getClass().getMethod(name);
                Object o = m.invoke(item);
                if (o instanceof Number n) return n.doubleValue();
            } catch (Exception ignored) {
            }
        }
        try {
            Method m = item.getClass().getMethod("getSellItem");
            Object o = m.invoke(item);
            if (o != null) return safeGetNumberAsDouble(o, "getPrice", "getCost", "getPriceDouble");
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static boolean safeGetBoolean(Object item, String... names) {
        for (String name : names) {
            try {
                Method m = item.getClass().getMethod(name);
                Object o = m.invoke(item);
                if (o instanceof Boolean b) return b;
            } catch (Exception ignored) {
            }
        }
        return false;
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
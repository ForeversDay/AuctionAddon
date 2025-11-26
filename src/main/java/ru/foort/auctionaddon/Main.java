package ru.foort.auctionaddon;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.bauction.bmenu.menu.MenuLoader;
import ru.foort.auctionaddon.commands.Command;
import ru.foort.auctionaddon.commands.TabComplete;
import ru.foort.auctionaddon.commands.impl.*;
import ru.foort.auctionaddon.events.BuyEvent;
import ru.foort.auctionaddon.events.OfflineBuyEvent;
import java.io.File;
import java.lang.reflect.Field;

public class Main extends JavaPlugin {
    private static Main instance;
    private Economy economy;
    private MenuLoader menuLoader;
    private AddItemsCommand addItemsCommand;
    private PSellCommand pSellCommand;
    private ResellCommand resellCommand;
    private SearchCommand searchCommand;
    private SellCommand sellCommand;
    private ViewCommand viewCommand;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("BAuction") == null) {
            getServer().getLogger().severe("Не найден плагин BAuction");
            this.setEnabled(false);
            return;
        }
        if (getServer().getPluginManager().getPlugin("BLib") == null) {
            getServer().getLogger().severe("Не найден плагин BLib");
            this.setEnabled(false);
            return;
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getServer().getLogger().severe("Не найден плагин Vault");
            this.setEnabled(false);
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            setEnabled(false);
            return;
        }
        economy = rsp.getProvider();
        instance = this;
        saveDefaultConfig();
        saveTranslations();
        initMenuLoader();
        getCommand("ah").setExecutor(new Command(this, menuLoader, getConfig().getString("menu_settings.home"), getConfig().getString("menu_settings.view")));
        getCommand("ah").setTabCompleter(new TabComplete());
        getServer().getPluginManager().registerEvents(new BuyEvent(this), this);
        getServer().getPluginManager().registerEvents(new OfflineBuyEvent(this), this);
    }

    private void initMenuLoader() {
        try {
            Class<?> baClass = Class.forName("org.by1337.bauction.Main");
            Object baInstance = baClass.getMethod("getInstance").invoke(null);
            try {
                Object ml = baClass.getMethod("getMenuLoader").invoke(baInstance);
                if (ml instanceof MenuLoader) {
                    menuLoader = (MenuLoader) ml;
                    return;
                }
            } catch (NoSuchMethodException ignored) {
            }

            for (Field f : baClass.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(baInstance);
                    if (val != null && val.getClass().getSimpleName().equals("MenuLoader") && val instanceof MenuLoader) {
                        menuLoader = (MenuLoader) val;
                        return;
                    }
                } catch (Throwable ignoredField) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void saveTranslations() {
        File file = new File(getDataFolder(), "ru_ru.json");
        if (!file.exists()) saveResource("ru_ru.json", false);
    }

    public static Main getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public MenuLoader getMenuLoader() {
        return menuLoader;
    }

    public AddItemsCommand getAddItemsCommand() {
        return addItemsCommand;
    }

    public PSellCommand getPSellCommand() {
        return pSellCommand;
    }

    public ResellCommand getResellCommand() {
        return resellCommand;
    }

    public SearchCommand getSearchCommand() {
        return searchCommand;
    }

    public SellCommand getSellCommand() {
        return sellCommand;
    }

    public ViewCommand getViewCommand() {
        return viewCommand;
    }
}

package ru.foort.auctionaddon;

import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.bauction.bmenu.menu.MenuLoader;
import ru.foort.auctionaddon.commands.AhCommand;
import ru.foort.auctionaddon.commands.AhTabComplete;
import ru.foort.auctionaddon.dsell.DSellListener;
import ru.foort.auctionaddon.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;

public class Main extends JavaPlugin {
    private static Main instance;
    private MenuLoader menuLoader;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveTranslations();
        initMenuLoader();
        Utils utils = new Utils();
        getCommand("ah").setExecutor(new AhCommand(this, menuLoader, getConfig().getString("menu_settings.home"), getConfig().getString("menu_settings.view"), utils));
        getCommand("ah").setTabCompleter(new AhTabComplete(utils));
        getServer().getPluginManager().registerEvents(new DSellListener(), this);
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
            } catch (NoSuchMethodException ignored) {}

            for (Field f : baClass.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(baInstance);
                    if (val != null && val.getClass().getSimpleName().equals("MenuLoader") && val instanceof MenuLoader) {
                        menuLoader = (MenuLoader) val;
                        return;
                    }
                } catch (Throwable ignoredField) {}
            }
        } catch (Throwable ignored) {}
    }

    private void saveTranslations() {
        File file = new File(getDataFolder(), "ru_ru.json");
        if (!file.exists()) saveResource("ru_ru.json", false);
    }

    public static Main getInstance() {
        return instance;
    }

    public MenuLoader getMenuLoader() {
        return menuLoader;
    }
}

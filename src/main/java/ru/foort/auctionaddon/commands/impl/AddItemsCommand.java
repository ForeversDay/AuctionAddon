package ru.foort.auctionaddon.commands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.Main;
import ru.foort.auctionaddon.utils.Color;
import ru.foort.auctionaddon.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class AddItemsCommand implements CommandExecutor {
    private final Main plugin;
    private final File itemsFile;
    private final FileConfiguration itemsConfig;

    public AddItemsCommand(Main plugin) {
        this.plugin = plugin;
        this.itemsFile = new File(plugin.getDataFolder(), "data/items.yml");
        if (!itemsFile.exists()) {
            itemsFile.getParentFile().mkdirs();
            try {
                itemsFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        this.itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cВася, зайди в игру, а не от консоли пиши команды!"));
            return true;
        }
        if (!player.hasPermission("aaddon.admin")) {
            player.sendMessage(Color.translate("&cУ вас нет прав на выполнение этой команды!"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.additems_usage")));
            return true;
        }

        String name = args[1].toLowerCase();
        String displayname = String.join("_", java.util.Arrays.copyOfRange(args, 2, args.length));

        itemsConfig.set(displayname, name);
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.loadTranslations(plugin);
        player.sendMessage(Color.translate("&aПредмет добавлен: &f" + displayname + " &a(название: " + name + ")"));
        return true;
    }
}
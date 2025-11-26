package ru.foort.auctionaddon.commands.impl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.by1337.bauction.Main;
import org.by1337.bauction.db.kernel.User;
import org.by1337.bauction.menu.PlayerItemsView;
import org.by1337.bauction.bmenu.menu.MenuLoader;
import ru.foort.auctionaddon.utils.Color;

import java.util.UUID;

public class ViewCommand implements CommandExecutor {
    private final ru.foort.auctionaddon.Main plugin;
    private final MenuLoader menuLoader;
    private final String viewMenuId;

    public ViewCommand(ru.foort.auctionaddon.Main plugin, MenuLoader menuLoader, String viewMenuId) {
        this.plugin = plugin;
        this.menuLoader = menuLoader;
        this.viewMenuId = viewMenuId;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Color.translate("&cВася, зайди в игру, а не от консоли пиши команды!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_player")));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = offlineTarget.getUniqueId();

        User user = Main.getStorage().getUser(uuid);
        if (user == null) {
            player.sendMessage(Color.translate(plugin.getConfig().getString("messages.no_player")));
            return true;
        }

        var menu = menuLoader.getMenu(viewMenuId);
        if (menu == null) {
            plugin.getLogger().severe(Color.translate("&cMenu: view не найдено!"));
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
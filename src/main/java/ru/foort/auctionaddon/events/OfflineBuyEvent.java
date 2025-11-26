package ru.foort.auctionaddon.events;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import ru.foort.auctionaddon.Main;
import ru.foort.auctionaddon.utils.Color;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class OfflineBuyEvent implements Listener {
    private final Main plugin;

    public OfflineBuyEvent(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File file = new File(plugin.getDataFolder(), "data/" + player.getUniqueId() + ".txt");
        if (file.exists()) {
            try {
                List<String> messages = Files.readAllLines(file.toPath());
                for (String msg : messages) {
                    player.sendMessage(Color.translate(msg));
                }
                file.delete();
            } catch (Exception ignored) {}
        }
    }
}
package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.misc.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;
    private final JavaPlugin plugin;

    public VanishListener(VanishManager vanishManager, JavaPlugin plugin) {
        this.vanishManager = vanishManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Apply vanish to new player — hide all currently vanished players from them
        vanishManager.applyVanishToNewPlayer(player);

        // If this joining player is staff with see permission, show them all vanished players
        if (player.hasPermission("bob.vanish.see")) {
            for (Player online : player.getServer().getOnlinePlayers()) {
                if (vanishManager.isVanished(online.getUniqueId())) {
                    player.showPlayer(plugin, online);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If they quit while vanished, suppress quit message and clean up
        if (vanishManager.isVanished(player.getUniqueId())) {
            event.quitMessage(null);
            vanishManager.onQuit(player);
        }
    }
}

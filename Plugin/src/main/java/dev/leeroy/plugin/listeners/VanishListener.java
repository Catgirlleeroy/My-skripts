package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.VanishManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;

    public VanishListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
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
                    player.showPlayer(online);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If they quit while vanished, clean up and suppress quit message
        if (vanishManager.isVanished(player.getUniqueId())) {
            event.setQuitMessage(null);
            vanishManager.onQuit(player);
        }
    }
}
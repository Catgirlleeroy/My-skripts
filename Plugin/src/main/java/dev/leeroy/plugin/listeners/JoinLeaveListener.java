package dev.leeroy.plugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinLeaveListener implements Listener {

    private final JavaPlugin plugin;

    public JoinLeaveListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-leave.join-enabled", true)) {
            event.setJoinMessage(null);
            return;
        }

        String raw = plugin.getConfig().getString("join-leave.join-message", "&a+ &e{player} &ajoined the server!");
        String message = ChatColor.translateAlternateColorCodes('&',
                raw.replace("{player}", event.getPlayer().getName()));
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("join-leave.leave-enabled", true)) {
            event.setQuitMessage(null);
            return;
        }

        String raw = plugin.getConfig().getString("join-leave.leave-message", "&c- &e{player} &cleft the server.");
        String message = ChatColor.translateAlternateColorCodes('&',
                raw.replace("{player}", event.getPlayer().getName()));
        event.setQuitMessage(message);
    }
}
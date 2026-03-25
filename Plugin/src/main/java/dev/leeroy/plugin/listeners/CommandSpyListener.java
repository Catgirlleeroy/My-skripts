package dev.leeroy.plugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpyListener implements Listener {

    // Players who have commandspy enabled
    private final Set<UUID> spying = new HashSet<>();

    /**
     * Toggles commandspy for a player.
     * @return true if now enabled, false if now disabled
     */
    public boolean toggle(Player player) {
        if (spying.contains(player.getUniqueId())) {
            spying.remove(player.getUniqueId());
            return false;
        } else {
            spying.add(player.getUniqueId());
            return true;
        }
    }

    public boolean isSpying(Player player) {
        return spying.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player executor = event.getPlayer();
        String message = event.getMessage(); // includes the leading /

        // Notify all players with commandspy enabled, except the one who ran the command
        for (Player spy : executor.getServer().getOnlinePlayers()) {
            if (!spy.getUniqueId().equals(executor.getUniqueId()) && isSpying(spy)) {
                spy.sendMessage(
                        ChatColor.GRAY + "[CommandSpy] " +
                                ChatColor.YELLOW + executor.getName() +
                                ChatColor.WHITE + " » " +
                                ChatColor.AQUA + message
                );
            }
        }
    }
}
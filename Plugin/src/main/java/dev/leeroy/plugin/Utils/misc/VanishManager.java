package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final Set<UUID> vanished = new HashSet<>();

    public boolean toggle(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            unvanish(player);
            return false;
        } else {
            vanish(player);
            return true;
        }
    }

    public void vanish(Player player) {
        vanished.add(player.getUniqueId());
        // Hide from all players who can't see vanished
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.hasPermission("bob.vanish.see") && !other.equals(player)) {
                other.hidePlayer(player);
            }
        }
    }

    public void unvanish(Player player) {
        vanished.remove(player.getUniqueId());
        // Show to everyone again
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(player);
        }
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /**
     * Called when a new player joins — hide all vanished players from them
     * unless they have the see permission.
     */
    public void applyVanishToNewPlayer(Player newPlayer) {
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !newPlayer.hasPermission("bob.vanish.see")) {
                newPlayer.hidePlayer(vanishedPlayer);
            }
        }
    }

    /**
     * Called when a vanished player quits — clean up their entry.
     */
    public void onQuit(Player player) {
        vanished.remove(player.getUniqueId());
    }
}
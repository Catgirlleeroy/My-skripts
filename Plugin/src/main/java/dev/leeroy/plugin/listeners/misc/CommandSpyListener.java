package dev.leeroy.plugin.listeners.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpyListener implements Listener {

    private final Set<UUID> spying = new HashSet<>();

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
        String message = event.getMessage();

        for (Player spy : executor.getServer().getOnlinePlayers()) {
            if (!spy.getUniqueId().equals(executor.getUniqueId()) && isSpying(spy)) {
                spy.sendMessage(Component.text("[CommandSpy] ", NamedTextColor.GRAY)
                        .append(Component.text(executor.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" » ", NamedTextColor.WHITE))
                        .append(Component.text(message, NamedTextColor.AQUA)));
            }
        }
    }
}

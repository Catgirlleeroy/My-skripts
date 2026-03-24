package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;

public class BanListener implements Listener {

    private final BanManager banManager;

    public BanListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        String name = event.getPlayer().getName();

        if (!banManager.isBanned(name)) return;

        Map<String, Object> details = banManager.getBanDetails(name);
        if (details == null) return; // expired and cleaned up between the two calls — let them in

        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        StringBuilder msg = new StringBuilder();
        msg.append(ChatColor.RED).append("You are banned from this server.\n");
        msg.append(ChatColor.WHITE).append("Reason: ").append(reason).append("\n");
        msg.append(ChatColor.WHITE).append("Banned by: ").append(bannedBy).append("\n");

        if (type.equals("temp") && expiry != -1L) {
            msg.append(ChatColor.WHITE).append("Expires in: ")
                    .append(BanManager.formatRemaining(expiry));
        } else {
            msg.append(ChatColor.WHITE).append("Duration: Permanent");
        }

        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, msg.toString());
    }
}
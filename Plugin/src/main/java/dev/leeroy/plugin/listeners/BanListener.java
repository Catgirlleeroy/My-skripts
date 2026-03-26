package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;
import java.util.UUID;

public class BanListener implements Listener {

    private final BanManager banManager;
    private final IPBanManager ipBanManager;

    public BanListener(BanManager banManager, IPBanManager ipBanManager) {
        this.banManager = banManager;
        this.ipBanManager = ipBanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        String ip   = event.getAddress().getHostAddress();

        // Check UUID ban
        if (banManager.isBanned(uuid)) {
            Map<String, Object> details = banManager.getBanDetails(uuid);
            if (details != null) {
                String type     = (String) details.get("type");
                String reason   = (String) details.get("reason");
                String bannedBy = (String) details.get("bannedBy");
                long   expiry   = (long)   details.get("expiry");

                notifyStaff(name, ip, "BAN", type, reason, bannedBy, expiry);
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        buildKickMessage("banned", type, reason, bannedBy, expiry));
                return;
            }
        }

        // Check IP ban
        if (ipBanManager.isBanned(ip)) {
            Map<String, Object> details = ipBanManager.getBanDetails(ip);
            if (details != null) {
                String type     = (String) details.get("type");
                String reason   = (String) details.get("reason");
                String bannedBy = (String) details.get("bannedBy");
                long   expiry   = (long)   details.get("expiry");

                notifyStaff(name, ip, "IP BAN", type, reason, bannedBy, expiry);
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        buildKickMessage("IP banned", type, reason, bannedBy, expiry));
            }
        }
    }

    private void notifyStaff(String playerName, String ip, String banType,
                             String type, String reason, String bannedBy, long expiry) {
        String header =
                ChatColor.RED + "[" + banType + "] " +
                        ChatColor.YELLOW + playerName +
                        ChatColor.GRAY + " (" + ip + ")" +
                        ChatColor.RED + " tried to join!";

        String details =
                ChatColor.YELLOW + "Type: " + ChatColor.WHITE + (type.equals("permanent") ? "Permanent" : "Temporary") +
                        ChatColor.YELLOW + " | Reason: " + ChatColor.WHITE + reason +
                        ChatColor.YELLOW + " | By: " + ChatColor.WHITE + bannedBy +
                        (expiry != -1L ? ChatColor.YELLOW + " | Expires in: " + ChatColor.WHITE + BanManager.formatRemaining(expiry) : "");

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("bob.staff"))
                .forEach(staff -> {
                    staff.sendMessage(header);
                    staff.sendMessage(details);
                });

        Bukkit.getLogger().info("[Bob] " + ChatColor.stripColor(header) + " " + ChatColor.stripColor(details));
    }

    private String buildKickMessage(String banLabel, String type, String reason, String bannedBy, long expiry) {
        StringBuilder msg = new StringBuilder();
        msg.append(ChatColor.RED).append("You are ").append(banLabel).append(" from this server.\n");
        msg.append(ChatColor.WHITE).append("Reason: ").append(reason).append("\n");
        msg.append(ChatColor.WHITE).append("Banned by: ").append(bannedBy).append("\n");
        if (type.equals("temp") && expiry != -1L) {
            msg.append(ChatColor.WHITE).append("Expires in: ").append(BanManager.formatRemaining(expiry));
        } else {
            msg.append(ChatColor.WHITE).append("Duration: Permanent");
        }
        return msg.toString();
    }
}
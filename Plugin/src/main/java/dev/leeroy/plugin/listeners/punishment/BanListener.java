package dev.leeroy.plugin.listeners.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanListener implements Listener {

    private final BanManager banManager;
    private final IPBanManager ipBanManager;
    private final PlayerCache playerCache;

    private static final long NOTIFY_COOLDOWN_MS = 10_000L;
    private final Map<UUID, Long> lastNotified = new HashMap<>();

    public BanListener(BanManager banManager, IPBanManager ipBanManager, PlayerCache playerCache) {
        this.banManager   = banManager;
        this.ipBanManager = ipBanManager;
        this.playerCache  = playerCache;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        String ip   = event.getAddress().getHostAddress();

        // ── Check UUID ban ───────────────────────────────────────────────────
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

        // ── Check IP ban ─────────────────────────────────────────────────────
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
                return;
            }
        }

        // ── Alt alert: same IP as a banned player ────────────────────────────
        for (UUID bannedUUID : banManager.getAllBannedUUIDs()) {
            if (bannedUUID.equals(uuid)) continue;

            String cachedIP = playerCache.getIP(bannedUUID);
            if (cachedIP != null && cachedIP.equals(ip)) {
                String bannedName = playerCache.getName(bannedUUID);
                Map<String, Object> details = banManager.getBanDetails(bannedUUID);
                if (details == null) continue;

                String reason   = (String) details.get("reason");
                String bannedBy = (String) details.get("bannedBy");

                Component header = TextUtil.parse(
                        "&6[ALT ALERT] &e" + name + " &7(" + ip + ")" +
                        "&6 joined on the same IP as banned player &c" +
                        (bannedName != null ? bannedName : bannedUUID) + "&6!");

                Component detail = TextUtil.parse(
                        "&eBanned reason: &f" + reason + " &e| Banned by: &f" + bannedBy);

                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("bob.staff"))
                        .forEach(staff -> { staff.sendMessage(header); staff.sendMessage(detail); });

                Bukkit.getLogger().info("[Bob] ALT ALERT: " + name + " (" + ip +
                        ") shares IP with banned " + bannedName + ". Reason: " + reason);
                break;
            }
        }
    }

    private void notifyStaff(String playerName, String ip, String banType,
                             String type, String reason, String bannedBy, long expiry) {
        UUID key = UUID.nameUUIDFromBytes(playerName.getBytes());
        long now = System.currentTimeMillis();
        if (lastNotified.containsKey(key) && now - lastNotified.get(key) < NOTIFY_COOLDOWN_MS) return;
        lastNotified.put(key, now);

        Component header = TextUtil.parse(
                "&c[" + banType + "] &e" + playerName + " &7(" + ip + ") &ctried to join!");

        Component details = TextUtil.parse(
                "&eType: &f" + (type.equals("permanent") ? "Permanent" : "Temporary") +
                " &e| Reason: &f" + reason +
                " &e| By: &f" + bannedBy +
                (expiry != -1L ? " &e| Expires in: &f" + BanManager.formatRemaining(expiry) : ""));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("bob.staff"))
                .forEach(staff -> { staff.sendMessage(header); staff.sendMessage(details); });

        Bukkit.getLogger().info("[Bob] [" + banType + "] " + playerName + " (" + ip +
                ") tried to join. Type: " + type + " Reason: " + reason);
    }

    private Component buildKickMessage(String banLabel, String type, String reason,
                                       String bannedBy, long expiry) {
        Component msg = Component.text("You are " + banLabel + " from this server.\n", NamedTextColor.RED)
                .append(Component.text("Reason: " + reason + "\n", NamedTextColor.WHITE))
                .append(Component.text("Banned by: " + bannedBy + "\n", NamedTextColor.WHITE));

        if (type.equals("temp") && expiry != -1L) {
            msg = msg.append(Component.text("Expires in: " + BanManager.formatRemaining(expiry), NamedTextColor.WHITE));
        } else {
            msg = msg.append(Component.text("Duration: Permanent", NamedTextColor.WHITE));
        }
        return msg;
    }
}

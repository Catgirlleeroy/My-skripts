package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class BanManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public BanManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void ban(UUID uuid, String name, String reason, String bannedBy) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO bans (uuid, name, type, reason, banned_by, expiry) KEY (uuid) VALUES (?, ?, 'permanent', ?, ?, -1)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, reason);
            ps.setString(4, bannedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] ban failed: " + e.getMessage());
        }
    }

    public void tempBan(UUID uuid, String name, String reason, String bannedBy, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO bans (uuid, name, type, reason, banned_by, expiry) KEY (uuid) VALUES (?, ?, 'temp', ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, reason);
            ps.setString(4, bannedBy);
            ps.setLong(5, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] tempBan failed: " + e.getMessage());
        }
    }

    public void unban(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] unban failed: " + e.getMessage());
        }
    }

    public boolean isBanned(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT expiry FROM bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unban(uuid);
                    return false;
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] isBanned failed: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getBanDetails(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT name, type, reason, banned_by, expiry FROM bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unban(uuid);
                    return null;
                }
                Map<String, Object> details = new HashMap<>();
                details.put("name",     rs.getString("name"));
                details.put("type",     rs.getString("type"));
                details.put("reason",   rs.getString("reason"));
                details.put("bannedBy", rs.getString("banned_by"));
                details.put("expiry",   expiry);
                return details;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] getBanDetails failed: " + e.getMessage());
            return null;
        }
    }

    public Set<UUID> getAllBannedUUIDs() {
        Set<UUID> uuids = new HashSet<>();
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT uuid, expiry FROM bans")) {
            while (rs.next()) {
                long expiry = rs.getLong("expiry");
                if (expiry == -1L || System.currentTimeMillis() <= expiry) {
                    uuids.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[BanManager] getAllBannedUUIDs failed: " + e.getMessage());
        }
        return uuids;
    }

    public void reload() { /* H2 is always current — no reload needed */ }

    // ── Duration utilities ────────────────────────────────────────────────────

    public static long parseDuration(String input) {
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) return -1;
                long value = Long.parseLong(num.toString());
                num.setLength(0);
                if      (c == 'd') total += value * 86_400_000L;
                else if (c == 'h') total += value * 3_600_000L;
                else if (c == 'm') total += value * 60_000L;
                else if (c == 's') total += value * 1_000L;
                else return -1;
            }
        }
        return total > 0 ? total : -1;
    }

    public static String formatRemaining(long expiryMs) {
        long remaining = expiryMs - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long seconds = remaining / 1000;
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}

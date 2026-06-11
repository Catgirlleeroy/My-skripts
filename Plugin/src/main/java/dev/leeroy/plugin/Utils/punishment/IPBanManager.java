package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class IPBanManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public IPBanManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void ban(String ip, String reason, String bannedBy) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO ip_bans (ip, type, reason, banned_by, expiry) KEY (ip) VALUES (?, 'permanent', ?, ?, -1)")) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setString(3, bannedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[IPBanManager] ban failed: " + e.getMessage());
        }
    }

    public void tempBan(String ip, String reason, String bannedBy, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO ip_bans (ip, type, reason, banned_by, expiry) KEY (ip) VALUES (?, 'temp', ?, ?, ?)")) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setString(3, bannedBy);
            ps.setLong(4, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[IPBanManager] tempBan failed: " + e.getMessage());
        }
    }

    public void unban(String ip) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ip_bans WHERE ip = ?")) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[IPBanManager] unban failed: " + e.getMessage());
        }
    }

    public boolean isBanned(String ip) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT expiry FROM ip_bans WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unban(ip);
                    return false;
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[IPBanManager] isBanned failed: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getBanDetails(String ip) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT type, reason, banned_by, expiry FROM ip_bans WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unban(ip);
                    return null;
                }
                Map<String, Object> details = new HashMap<>();
                details.put("ip",       ip);
                details.put("type",     rs.getString("type"));
                details.put("reason",   rs.getString("reason"));
                details.put("bannedBy", rs.getString("banned_by"));
                details.put("expiry",   expiry);
                return details;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[IPBanManager] getBanDetails failed: " + e.getMessage());
            return null;
        }
    }

    public void reload() { /* H2 is always current */ }
}

package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class MuteManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public MuteManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void mute(UUID uuid, String name, String reason, String mutedBy) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO mutes (uuid, name, type, reason, muted_by, expiry) KEY (uuid) VALUES (?, ?, 'permanent', ?, ?, -1)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, reason);
            ps.setString(4, mutedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MuteManager] mute failed: " + e.getMessage());
        }
    }

    public void tempMute(UUID uuid, String name, String reason, String mutedBy, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO mutes (uuid, name, type, reason, muted_by, expiry) KEY (uuid) VALUES (?, ?, 'temp', ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, reason);
            ps.setString(4, mutedBy);
            ps.setLong(5, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MuteManager] tempMute failed: " + e.getMessage());
        }
    }

    public void unmute(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM mutes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[MuteManager] unmute failed: " + e.getMessage());
        }
    }

    public boolean isMuted(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT expiry FROM mutes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unmute(uuid);
                    return false;
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MuteManager] isMuted failed: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getMuteDetails(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT name, type, reason, muted_by, expiry FROM mutes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long expiry = rs.getLong("expiry");
                if (expiry != -1L && System.currentTimeMillis() > expiry) {
                    unmute(uuid);
                    return null;
                }
                Map<String, Object> details = new HashMap<>();
                details.put("name",    rs.getString("name"));
                details.put("type",    rs.getString("type"));
                details.put("reason",  rs.getString("reason"));
                details.put("mutedBy", rs.getString("muted_by"));
                details.put("expiry",  expiry);
                return details;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[MuteManager] getMuteDetails failed: " + e.getMessage());
            return null;
        }
    }

    public void reload() { /* H2 is always current */ }
}

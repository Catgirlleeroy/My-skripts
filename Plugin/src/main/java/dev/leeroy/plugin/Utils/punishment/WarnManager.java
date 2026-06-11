package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;

public class WarnManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public WarnManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void reload() { /* H2 is always current */ }

    public int getWarns(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT warns FROM warns WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("warns") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] getWarns failed: " + e.getMessage());
            return 0;
        }
    }

    public int addWarn(UUID uuid) {
        int current = getWarns(uuid) + 1;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO warns (uuid, warns) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, current);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] addWarn failed: " + e.getMessage());
        }
        return current;
    }

    public int removeWarn(UUID uuid) {
        int current = Math.max(0, getWarns(uuid) - 1);
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO warns (uuid, warns) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, current);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] removeWarn failed: " + e.getMessage());
        }
        return current;
    }

    public void resetWarns(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO warns (uuid, warns) KEY (uuid) VALUES (?, 0)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] resetWarns failed: " + e.getMessage());
        }
    }

    public int getOffenses(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT offenses FROM warns WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("offenses") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] getOffenses failed: " + e.getMessage());
            return 0;
        }
    }

    public void incrementOffenses(UUID uuid) {
        int current = getOffenses(uuid) + 1;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO warns (uuid, offenses) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, current);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[WarnManager] incrementOffenses failed: " + e.getMessage());
        }
    }
}

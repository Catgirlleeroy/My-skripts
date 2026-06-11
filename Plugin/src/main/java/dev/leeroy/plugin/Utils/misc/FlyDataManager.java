package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FlyDataManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public FlyDataManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void reload() { /* H2 is always current */ }

    // ── Time ──────────────────────────────────────────────────────────────────

    public long getTime(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT time_seconds FROM fly_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("time_seconds") : 0L;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] getTime failed: " + e.getMessage());
            return 0L;
        }
    }

    public void setTime(UUID uuid, long seconds) {
        long val = Math.max(0, seconds);
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO fly_data (uuid, time_seconds) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, val);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] setTime failed: " + e.getMessage());
        }
    }

    public void addTime(UUID uuid, long seconds)    { setTime(uuid, getTime(uuid) + seconds); }
    public void removeTime(UUID uuid, long seconds) { setTime(uuid, Math.max(0, getTime(uuid) - seconds)); }

    // ── Permanent fly ─────────────────────────────────────────────────────────

    public boolean hasPermanentFly(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT permanent FROM fly_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("permanent");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] hasPermanentFly failed: " + e.getMessage());
            return false;
        }
    }

    public void setPermanentFly(UUID uuid, boolean value) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO fly_data (uuid, permanent) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setBoolean(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] setPermanentFly failed: " + e.getMessage());
        }
    }

    // ── Daily reset ───────────────────────────────────────────────────────────

    public String getLastLoginDate(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT last_login_date FROM fly_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("last_login_date") : "";
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] getLastLoginDate failed: " + e.getMessage());
            return "";
        }
    }

    public void setLastLoginDate(UUID uuid, String date) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO fly_data (uuid, last_login_date) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] setLastLoginDate failed: " + e.getMessage());
        }
    }

    public boolean hasReceivedDailyBonus(UUID uuid) {
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE).equals(getLastLoginDate(uuid));
    }

    public void markDailyBonus(UUID uuid) {
        setLastLoginDate(uuid, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
    }

    // ── First join ────────────────────────────────────────────────────────────

    public boolean hasJoinedBefore(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT uuid FROM fly_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] hasJoinedBefore failed: " + e.getMessage());
            return false;
        }
    }

    public void markFirstJoin(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO fly_data (uuid, first_join) KEY (uuid) VALUES (?, TRUE)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[FlyDataManager] markFirstJoin failed: " + e.getMessage());
        }
    }
}

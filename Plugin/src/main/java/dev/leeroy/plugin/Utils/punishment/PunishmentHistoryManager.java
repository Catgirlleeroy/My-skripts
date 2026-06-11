package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PunishmentHistoryManager {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public PunishmentHistoryManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void reload() { /* H2 is always current */ }

    public void log(UUID uuid, String targetName, String type, String reason, String punisher, String duration) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO punishment_history (uuid, target_name, type, reason, punisher, duration, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, targetName);
            ps.setString(3, type);
            ps.setString(4, reason);
            ps.setString(5, punisher);
            ps.setString(6, duration != null ? duration : "permanent");
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[PunishmentHistoryManager] log failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getHistory(UUID uuid) {
        List<Map<String, Object>> entries = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT target_name, type, reason, punisher, duration, timestamp " +
                 "FROM punishment_history WHERE uuid = ? ORDER BY id ASC")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                int index = 1;
                while (rs.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("index",     index++);
                    entry.put("name",      rs.getString("target_name"));
                    entry.put("type",      rs.getString("type"));
                    entry.put("reason",    rs.getString("reason"));
                    entry.put("punisher",  rs.getString("punisher"));
                    entry.put("duration",  rs.getString("duration"));
                    long ts = rs.getLong("timestamp");
                    entry.put("timestamp", ts > 0 ? DATE_FMT.format(new java.util.Date(ts)) : "Unknown");
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[PunishmentHistoryManager] getHistory failed: " + e.getMessage());
        }
        return entries;
    }

    public void clearHistory(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM punishment_history WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[PunishmentHistoryManager] clearHistory failed: " + e.getMessage());
        }
    }
}

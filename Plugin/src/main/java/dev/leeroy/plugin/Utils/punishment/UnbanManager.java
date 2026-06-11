package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class UnbanManager {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public UnbanManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void reload() { /* H2 is always current */ }

    public void log(UUID uuid, String targetName, String unbannedBy) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO unban_history (uuid, target_name, unbanned_by, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, targetName);
            ps.setString(3, unbannedBy);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[UnbanManager] log failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getHistory(UUID uuid) {
        List<Map<String, Object>> entries = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT target_name, unbanned_by, timestamp FROM unban_history WHERE uuid = ? ORDER BY id ASC")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                int index = 1;
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("index",      index++);
                    map.put("name",       rs.getString("target_name"));
                    map.put("unbannedBy", rs.getString("unbanned_by"));
                    long ts = rs.getLong("timestamp");
                    map.put("timestamp",  ts > 0 ? DATE_FMT.format(new java.util.Date(ts)) : "Unknown");
                    entries.add(map);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[UnbanManager] getHistory failed: " + e.getMessage());
        }
        return entries;
    }
}

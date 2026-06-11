package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    private final ConcurrentHashMap<UUID, String> names      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> ips        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> nameToUUID = new ConcurrentHashMap<>();

    public PlayerCache(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
        load();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void store(UUID uuid, String name) {
        names.put(uuid, name);
        nameToUUID.put(name.toLowerCase(), uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "MERGE INTO player_cache (uuid, name) KEY (uuid) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[PlayerCache] Failed to store name: " + e.getMessage());
            }
        });
    }

    public void storeIP(UUID uuid, String ip) {
        ips.put(uuid, ip);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "MERGE INTO player_cache (uuid, name, ip) KEY (uuid) VALUES (?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, names.getOrDefault(uuid, "Unknown"));
                ps.setString(3, ip);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[PlayerCache] Failed to store IP: " + e.getMessage());
            }
        });
    }

    public String getName(UUID uuid)         { return names.get(uuid); }
    public String getIP(UUID uuid)           { return ips.get(uuid); }
    public UUID   getUUID(String name)       { return nameToUUID.get(name.toLowerCase()); }
    public Collection<String> getAllNames()  { return names.values(); }

    public String getIPByName(String name) {
        UUID uuid = getUUID(name);
        return uuid != null ? getIP(uuid) : null;
    }

    public UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return getUUID(input);
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private void load() {
        try (Connection c = db.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT uuid, name, ip FROM player_cache")) {
            while (rs.next()) {
                UUID uuid   = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                String ip   = rs.getString("ip");
                names.put(uuid, name);
                nameToUUID.put(name.toLowerCase(), uuid);
                if (ip != null) ips.put(uuid, ip);
            }
            plugin.getLogger().info("[PlayerCache] Loaded " + names.size() + " players from H2.");
        } catch (SQLException e) {
            plugin.getLogger().severe("[PlayerCache] Failed to load from H2: " + e.getMessage());
        }
    }

    /** No-op — writes go directly to H2 on every update. */
    public void saveNow() {}

    public void reload() {
        names.clear();
        ips.clear();
        nameToUUID.clear();
        load();
    }
}

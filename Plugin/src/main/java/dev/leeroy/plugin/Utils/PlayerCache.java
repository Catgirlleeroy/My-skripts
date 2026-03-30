package dev.leeroy.plugin.Utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerCache {

    private final JavaPlugin plugin;
    private final File cacheFile;
    private YamlConfiguration config;

    public PlayerCache(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cacheFile = new File(plugin.getDataFolder(), "playercache.yml");
        load();
    }

    private void load() {
        if (!cacheFile.exists()) {
            try { cacheFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(cacheFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(cacheFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    /**
     * Store a player entry. Structure:
     *
     * players:
     *   <uuid>:
     *     name: PlayerName
     *     ip: 192.168.1.1
     */
    public void store(UUID uuid, String name) {
        String key = "players." + uuid.toString();
        config.set(key + ".name", name);
        // Also keep a name → uuid index for fast reverse lookup
        config.set("index." + name.toLowerCase(), uuid.toString());
        save();
    }

    public void storeIP(UUID uuid, String ip) {
        config.set("players." + uuid.toString() + ".ip", ip);
        save();
    }

    public String getName(UUID uuid) {
        return config.getString("players." + uuid.toString() + ".name", null);
    }

    public String getIP(UUID uuid) {
        return config.getString("players." + uuid.toString() + ".ip", null);
    }

    public String getIPByName(String name) {
        UUID uuid = getUUID(name);
        if (uuid == null) return null;
        return getIP(uuid);
    }

    public UUID getUUID(String name) {
        String raw = config.getString("index." + name.toLowerCase(), null);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException e) { return null; }
    }
}
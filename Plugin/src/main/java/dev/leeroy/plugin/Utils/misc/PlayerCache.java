package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final JavaPlugin plugin;
    private final File cacheFile;

    // Real in-memory maps — all reads/writes are instant, no YAML overhead
    // ConcurrentHashMap so async save thread can iterate safely while main
    // thread makes updates
    private final ConcurrentHashMap<UUID, String> names       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> ips         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> nameToUUID  = new ConcurrentHashMap<>();

    // Dirty flag — true means unsaved changes exist
    // Avoids unnecessary disk writes if nothing changed
    private volatile boolean dirty = false;

    public PlayerCache(JavaPlugin plugin) {
        this.plugin   = plugin;
        this.cacheFile = new File(plugin.getDataFolder(), "playercache.yml");

        // Load synchronously on startup — safe, happens before server accepts players
        load();

        // Autosave every 5 minutes (6000 ticks) — batch writes instead of per-player
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::asyncSave, 6000L, 6000L);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void store(UUID uuid, String name) {
        names.put(uuid, name);
        nameToUUID.put(name.toLowerCase(), uuid);
        dirty = true;
        // No disk write here — autosave handles it
    }

    public void storeIP(UUID uuid, String ip) {
        ips.put(uuid, ip);
        dirty = true;
    }

    public String getName(UUID uuid) {
        return names.get(uuid);
    }

    public String getIP(UUID uuid) {
        return ips.get(uuid);
    }

    public UUID getUUID(String name) {
        return nameToUUID.get(name.toLowerCase());
    }

    public String getIPByName(String name) {
        UUID uuid = getUUID(name);
        return uuid != null ? getIP(uuid) : null;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    /**
     * Load from disk into memory maps.
     * Always called synchronously — YAML loading is not thread-safe.
     */
    private void load() {
        if (!cacheFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);

        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = config.getString("players." + key + ".name");
                    String ip   = config.getString("players." + key + ".ip");

                    if (name != null) {
                        names.put(uuid, name);
                        nameToUUID.put(name.toLowerCase(), uuid);
                    }
                    if (ip != null) {
                        ips.put(uuid, ip);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUID keys
                }
            }
        }

        plugin.getLogger().info("[PlayerCache] Loaded " + names.size() + " players into memory.");
    }

    /**
     * Serialize current Maps into a fresh YamlConfiguration and save to disk.
     * Safe to call from async thread — we snapshot the Maps into a NEW config
     * object, so the main thread's Maps are never blocked or corrupted.
     */
    private void asyncSave() {
        if (!dirty) return; // Nothing changed — skip disk write entirely

        // Snapshot Maps into local variables first so we iterate a consistent state
        Map<UUID, String> nameSnap = new HashMap<>(names);
        Map<UUID, String> ipSnap   = new HashMap<>(ips);

        // Build a brand new YamlConfiguration — never touch the main thread's data
        YamlConfiguration snapshot = new YamlConfiguration();

        for (Map.Entry<UUID, String> entry : nameSnap.entrySet()) {
            String path = "players." + entry.getKey().toString();
            snapshot.set(path + ".name", entry.getValue());
            snapshot.set("index." + entry.getValue().toLowerCase(), entry.getKey().toString());

            String ip = ipSnap.get(entry.getKey());
            if (ip != null) snapshot.set(path + ".ip", ip);
        }

        try {
            snapshot.save(cacheFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("[PlayerCache] Failed to save: " + e.getMessage());
        }
    }

    /**
     * Final save on server shutdown — synchronous so data isn't lost.
     * Called from Plugin.onDisable() on the main thread.
     */
    public void saveNow() {
        dirty = true; // Force save regardless
        asyncSave();  // asyncSave() builds a new config so it's safe here too
    }

    /**
     * Reload from disk — synchronous and safe.
     * Clears Maps first to avoid stale data.
     */
    public void reload() {
        names.clear();
        ips.clear();
        nameToUUID.clear();
        load();
    }
}
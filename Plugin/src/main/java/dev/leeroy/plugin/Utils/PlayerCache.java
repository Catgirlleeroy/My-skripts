package dev.leeroy.plugin.Utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerCache {

    private final File cacheFile;
    private YamlConfiguration config;

    public PlayerCache(JavaPlugin plugin) {
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
        try { config.save(cacheFile); } catch (IOException e) { e.printStackTrace(); }
    }

    /** Store a UUID → name mapping when a player joins. */
    public void store(UUID uuid, String name) {
        config.set(uuid.toString(), name);
        // Also store name → UUID for reverse lookup
        config.set("names." + name.toLowerCase(), uuid.toString());
        save();
    }

    /** Get a player's last known name by UUID. */
    public String getName(UUID uuid) {
        return config.getString(uuid.toString(), null);
    }

    /** Get a player's UUID by their last known name. */
    public UUID getUUID(String name) {
        String raw = config.getString("names." + name.toLowerCase(), null);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException e) { return null; }
    }
}
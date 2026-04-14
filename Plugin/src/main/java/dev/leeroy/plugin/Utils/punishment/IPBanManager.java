package dev.leeroy.plugin.Utils.punishment;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IPBanManager {

    private final JavaPlugin plugin;
    private final File ipBanFile;
    private YamlConfiguration config;

    public IPBanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ipBanFile = new File(plugin.getDataFolder(), "ipbans.yml");
        load();
    }

    // ── Internal load/save ───────────────────────────────────────────────────

    private void load() {
        if (!ipBanFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { ipBanFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(ipBanFile);
    }

    /** Reloads ipbans.yml from disk, discarding any cached state. */
    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(ipBanFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    // ── Key helper — dots in IPs break YAML keys so we replace them ──────────
    private String toKey(String ip) {
        return ip.replace(".", "_");
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void ban(String ip, String reason, String bannedBy) {
        String key = toKey(ip);
        config.set(key + ".ip",       ip);
        config.set(key + ".type",     "permanent");
        config.set(key + ".reason",   reason);
        config.set(key + ".bannedBy", bannedBy);
        config.set(key + ".expiry",   -1L);
        save();
    }

    public void tempBan(String ip, String reason, String bannedBy, long durationMs) {
        String key = toKey(ip);
        long expiry = System.currentTimeMillis() + durationMs;
        config.set(key + ".ip",       ip);
        config.set(key + ".type",     "temp");
        config.set(key + ".reason",   reason);
        config.set(key + ".bannedBy", bannedBy);
        config.set(key + ".expiry",   expiry);
        save();
    }

    public void unban(String ip) {
        config.set(toKey(ip), null);
        save();
    }

    public boolean isBanned(String ip) {
        String key = toKey(ip);
        if (!config.contains(key)) return false;

        long expiry = config.getLong(key + ".expiry", -1L);
        if (expiry != -1L && System.currentTimeMillis() > expiry) {
            unban(ip);
            return false;
        }
        return true;
    }

    public Map<String, Object> getBanDetails(String ip) {
        String key = toKey(ip);
        if (!isBanned(ip)) return null;

        Map<String, Object> details = new HashMap<>();
        details.put("ip",       config.getString(key + ".ip"));
        details.put("type",     config.getString(key + ".type"));
        details.put("reason",   config.getString(key + ".reason"));
        details.put("bannedBy", config.getString(key + ".bannedBy"));
        details.put("expiry",   config.getLong(key + ".expiry", -1L));
        return details;
    }
}

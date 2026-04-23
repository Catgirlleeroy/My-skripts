package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.YamlDataStore;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class IPBanManager extends YamlDataStore {

    public IPBanManager(JavaPlugin plugin) {
        super(plugin, "ipbans.yml");
    }

    // Dots in IPs break YAML keys so we replace them
    private String toKey(String ip) {
        return ip.replace(".", "_");
    }

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

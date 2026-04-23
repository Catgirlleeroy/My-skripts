package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.YamlDataStore;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteManager extends YamlDataStore {

    public MuteManager(JavaPlugin plugin) {
        super(plugin, "mutes.yml");
    }

    public void mute(UUID uuid, String name, String reason, String mutedBy) {
        String key = uuid.toString();
        config.set(key + ".name",     name);
        config.set(key + ".type",     "permanent");
        config.set(key + ".reason",   reason);
        config.set(key + ".mutedBy",  mutedBy);
        config.set(key + ".expiry",   -1L);
        save();
    }

    public void tempMute(UUID uuid, String name, String reason, String mutedBy, long durationMs) {
        String key = uuid.toString();
        long expiry = System.currentTimeMillis() + durationMs;
        config.set(key + ".name",     name);
        config.set(key + ".type",     "temp");
        config.set(key + ".reason",   reason);
        config.set(key + ".mutedBy",  mutedBy);
        config.set(key + ".expiry",   expiry);
        save();
    }

    public void unmute(UUID uuid) {
        config.set(uuid.toString(), null);
        save();
    }

    public boolean isMuted(UUID uuid) {
        String key = uuid.toString();
        if (!config.contains(key)) return false;

        long expiry = config.getLong(key + ".expiry", -1L);
        if (expiry != -1L && System.currentTimeMillis() > expiry) {
            unmute(uuid);
            return false;
        }
        return true;
    }

    public Map<String, Object> getMuteDetails(UUID uuid) {
        String key = uuid.toString();
        if (!isMuted(uuid)) return null;

        Map<String, Object> details = new HashMap<>();
        details.put("name",    config.getString(key + ".name"));
        details.put("type",    config.getString(key + ".type"));
        details.put("reason",  config.getString(key + ".reason"));
        details.put("mutedBy", config.getString(key + ".mutedBy"));
        details.put("expiry",  config.getLong(key + ".expiry", -1L));
        return details;
    }
}

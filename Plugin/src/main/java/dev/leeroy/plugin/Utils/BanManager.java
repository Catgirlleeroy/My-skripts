package dev.leeroy.plugin.Utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanManager {

    private final JavaPlugin plugin;
    private final File banFile;
    private YamlConfiguration config;

    public BanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.banFile = new File(plugin.getDataFolder(), "bans.yml");
        load();
    }

    // ── Internal load/save ───────────────────────────────────────────────────

    private void load() {
        if (!banFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { banFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(banFile);
    }

    private void save() {
        try { config.save(banFile); } catch (IOException e) { e.printStackTrace(); }
    }

    /** Reloads bans.yml from disk, discarding any cached state. */
    public void reload() {
        load();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void ban(UUID uuid, String name, String reason, String bannedBy) {
        String key = uuid.toString();
        config.set(key + ".name",     name);
        config.set(key + ".type",     "permanent");
        config.set(key + ".reason",   reason);
        config.set(key + ".bannedBy", bannedBy);
        config.set(key + ".expiry",   -1L);
        save();
    }

    public void tempBan(UUID uuid, String name, String reason, String bannedBy, long durationMs) {
        String key = uuid.toString();
        long expiry = System.currentTimeMillis() + durationMs;
        config.set(key + ".name",     name);
        config.set(key + ".type",     "temp");
        config.set(key + ".reason",   reason);
        config.set(key + ".bannedBy", bannedBy);
        config.set(key + ".expiry",   expiry);
        save();
    }

    public void unban(UUID uuid) {
        config.set(uuid.toString(), null);
        save();
    }

    public boolean isBanned(UUID uuid) {
        String key = uuid.toString();
        if (!config.contains(key)) return false;

        long expiry = config.getLong(key + ".expiry", -1L);
        if (expiry != -1L && System.currentTimeMillis() > expiry) {
            unban(uuid);
            return false;
        }
        return true;
    }

    public Map<String, Object> getBanDetails(UUID uuid) {
        String key = uuid.toString();
        if (!isBanned(uuid)) return null;

        Map<String, Object> details = new HashMap<>();
        details.put("name",     config.getString(key + ".name"));
        details.put("type",     config.getString(key + ".type"));
        details.put("reason",   config.getString(key + ".reason"));
        details.put("bannedBy", config.getString(key + ".bannedBy"));
        details.put("expiry",   config.getLong(key + ".expiry", -1L));
        return details;
    }

    // ── Duration parser ──────────────────────────────────────────────────────

    public static long parseDuration(String input) {
        long total = 0;
        StringBuilder num = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) return -1;
                long value = Long.parseLong(num.toString());
                num.setLength(0);
                if      (c == 'd') total += value * 86_400_000L;
                else if (c == 'h') total += value * 3_600_000L;
                else if (c == 'm') total += value * 60_000L;
                else if (c == 's') total += value * 1_000L;
                else return -1;
            }
        }

        return total > 0 ? total : -1;
    }

    public static String formatRemaining(long expiryMs) {
        long remaining = expiryMs - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";

        long seconds = remaining / 1000;
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
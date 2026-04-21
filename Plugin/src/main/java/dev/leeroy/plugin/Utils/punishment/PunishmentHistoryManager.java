package dev.leeroy.plugin.Utils.punishment;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PunishmentHistoryManager {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final File historyFile;
    private YamlConfiguration config;

    public PunishmentHistoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        load();
    }

    private void load() {
        if (!historyFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { historyFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(historyFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(historyFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void log(UUID uuid, String targetName, String type, String reason, String punisher, String duration) {
        String base = uuid.toString();
        int count = config.getInt(base + ".count", 0);
        String entryBase = base + "." + count;

        config.set(entryBase + ".name",      targetName);
        config.set(entryBase + ".type",      type);
        config.set(entryBase + ".reason",    reason);
        config.set(entryBase + ".punisher",  punisher);
        config.set(entryBase + ".duration",  duration != null ? duration : "permanent");
        config.set(entryBase + ".timestamp", System.currentTimeMillis());
        config.set(base + ".count",          count + 1);
        save();
    }

    public List<Map<String, Object>> getHistory(UUID uuid) {
        String base = uuid.toString();
        int count = config.getInt(base + ".count", 0);
        List<Map<String, Object>> entries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String entryBase = base + "." + i;
            if (!config.contains(entryBase + ".type")) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index",     i + 1);
            entry.put("name",      config.getString(entryBase + ".name", "Unknown"));
            entry.put("type",      config.getString(entryBase + ".type", "?"));
            entry.put("reason",    config.getString(entryBase + ".reason", "No reason"));
            entry.put("punisher",  config.getString(entryBase + ".punisher", "Console"));
            entry.put("duration",  config.getString(entryBase + ".duration", "permanent"));
            long ts = config.getLong(entryBase + ".timestamp", 0L);
            entry.put("timestamp", ts > 0 ? DATE_FMT.format(new Date(ts)) : "Unknown");
            entries.add(entry);
        }
        return entries;
    }

    public void clearHistory(UUID uuid) {
        config.set(uuid.toString(), null);
        save();
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }
}

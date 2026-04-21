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

public class UnbanManager {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final File unbanFile;
    private YamlConfiguration config;

    public UnbanManager(JavaPlugin plugin) {
        this.plugin    = plugin;
        this.unbanFile = new File(plugin.getDataFolder(), "unban.yml");
        load();
    }

    private void load() {
        if (!unbanFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { unbanFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(unbanFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(unbanFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void log(UUID uuid, String targetName, String unbannedBy) {
        String base  = uuid.toString();
        int count    = config.getInt(base + ".count", 0);
        String entry = base + "." + count;

        config.set(entry + ".name",       targetName);
        config.set(entry + ".unbannedBy", unbannedBy);
        config.set(entry + ".timestamp",  System.currentTimeMillis());
        config.set(base  + ".count",      count + 1);
        save();
    }

    public List<Map<String, Object>> getHistory(UUID uuid) {
        String base  = uuid.toString();
        int count    = config.getInt(base + ".count", 0);
        List<Map<String, Object>> entries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String entry = base + "." + i;
            if (!config.contains(entry + ".unbannedBy")) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("index",      i + 1);
            map.put("name",       config.getString(entry + ".name",       "Unknown"));
            map.put("unbannedBy", config.getString(entry + ".unbannedBy", "Console"));
            long ts = config.getLong(entry + ".timestamp", 0L);
            map.put("timestamp",  ts > 0 ? DATE_FMT.format(new Date(ts)) : "Unknown");
            entries.add(map);
        }
        return entries;
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }
}

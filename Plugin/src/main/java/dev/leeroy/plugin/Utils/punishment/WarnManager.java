package dev.leeroy.plugin.Utils.punishment;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class WarnManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public WarnManager(JavaPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "warns.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    public int getWarns(UUID uuid) {
        String key = uuid.toString();
        // Migrate flat format (old) → nested format (new)
        if (config.isInt(key)) {
            int old = config.getInt(key);
            config.set(key, null);
            config.set(key + ".warns", old);
            config.set(key + ".offenses", 0);
            save();
        }
        return config.getInt(key + ".warns", 0);
    }

    public int addWarn(UUID uuid) {
        int current = getWarns(uuid) + 1;
        config.set(uuid + ".warns", current);
        save();
        return current;
    }

    public int removeWarn(UUID uuid) {
        int current = Math.max(0, getWarns(uuid) - 1);
        config.set(uuid + ".warns", current);
        save();
        return current;
    }

    public void resetWarns(UUID uuid) {
        config.set(uuid + ".warns", 0);
        save();
    }

    public int getOffenses(UUID uuid) {
        return config.getInt(uuid + ".offenses", 0);
    }

    public void incrementOffenses(UUID uuid) {
        config.set(uuid + ".offenses", getOffenses(uuid) + 1);
        save();
    }
}

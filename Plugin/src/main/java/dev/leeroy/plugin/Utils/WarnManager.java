package dev.leeroy.plugin.Utils;

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
        return config.getInt(uuid.toString(), 0);
    }

    public int addWarn(UUID uuid) {
        int current = getWarns(uuid) + 1;
        config.set(uuid.toString(), current);
        save();
        return current;
    }

    public int removeWarn(UUID uuid) {
        int current = Math.max(0, getWarns(uuid) - 1);
        config.set(uuid.toString(), current);
        save();
        return current;
    }

    public void resetWarns(UUID uuid) {
        config.set(uuid.toString(), 0);
        save();
    }
}
package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public abstract class YamlDataStore {

    protected final JavaPlugin plugin;
    protected final File file;
    protected YamlConfiguration config;

    protected YamlDataStore(JavaPlugin plugin, String filename) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), filename);
        load();
    }

    protected void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    protected void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(file); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }
}

package dev.leeroy.plugin.Utils.punishment;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class PunishConfig {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PunishConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "punish.yml");
        load();
    }

    private void load() {
        // Copy default punish.yml from jar if it doesn't exist
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("punish.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        load();
    }

    public YamlConfiguration get() {
        return config;
    }
}

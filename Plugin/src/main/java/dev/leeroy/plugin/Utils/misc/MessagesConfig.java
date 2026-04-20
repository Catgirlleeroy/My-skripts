package dev.leeroy.plugin.Utils.misc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MessagesConfig {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "messages.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("messages.yml")) {
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

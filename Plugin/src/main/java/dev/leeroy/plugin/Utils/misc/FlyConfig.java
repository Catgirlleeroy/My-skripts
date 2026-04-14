package dev.leeroy.plugin.Utils.misc;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class FlyConfig {

    private final JavaPlugin plugin;

    public FlyConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public org.bukkit.configuration.ConfigurationSection get() {
        return plugin.getConfig().getConfigurationSection("fly");
    }

    public String msg(String path) {
        String prefix = plugin.getConfig().getString("fly.messages.prefix", "&8[&dFly&8] ");
        String raw    = plugin.getConfig().getString("fly.messages." + path, "&cMissing: " + path);
        return colorize(prefix + raw);
    }

    public static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
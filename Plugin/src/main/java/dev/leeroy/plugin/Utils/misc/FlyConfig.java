package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

public class FlyConfig {

    private final JavaPlugin plugin;

    public FlyConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public org.bukkit.configuration.ConfigurationSection get() {
        return plugin.getConfig().getConfigurationSection("fly");
    }

    /** Returns the raw & color-coded message (prefix + body). Pass to TextUtil.parse(). */
    public String msg(String path) {
        String prefix = plugin.getConfig().getString("fly.messages.prefix", "&8[&dFly&8] ");
        String raw    = plugin.getConfig().getString("fly.messages." + path, "&cMissing: " + path);
        return prefix + raw;
    }

    /** Returns just the raw & color-coded prefix string. */
    public String prefix() {
        return plugin.getConfig().getString("fly.messages.prefix", "&8[&dFly&8] ");
    }
}
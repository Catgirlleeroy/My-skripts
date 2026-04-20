package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

public class FlyConfig {

    private final JavaPlugin plugin;
    private final MessagesConfig messagesConfig;

    public FlyConfig(JavaPlugin plugin, MessagesConfig messagesConfig) {
        this.plugin         = plugin;
        this.messagesConfig = messagesConfig;
    }

    public org.bukkit.configuration.ConfigurationSection get() {
        return plugin.getConfig().getConfigurationSection("fly");
    }

    /** Returns the raw & color-coded message (prefix + body). Pass to TextUtil.parse(). */
    public String msg(String path) {
        String prefix = messagesConfig.get().getString("fly.messages.prefix", "&8[&dFly&8] ");
        String raw    = messagesConfig.get().getString("fly.messages." + path, "&cMissing: " + path);
        return prefix + raw;
    }

    /** Returns just the raw & color-coded prefix string. */
    public String prefix() {
        return messagesConfig.get().getString("fly.messages.prefix", "&8[&dFly&8] ");
    }
}
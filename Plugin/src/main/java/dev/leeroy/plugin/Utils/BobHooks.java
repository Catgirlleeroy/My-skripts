package dev.leeroy.plugin.Utils;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central hook manager for all soft-dependent plugins.
 * Call BobHooks.init(plugin) once in onEnable() — it checks each plugin,
 * logs a clear success/failure message, and exposes static flags that the
 * rest of Bob uses to guard any calls into those APIs.
 */
public class BobHooks {

    private static boolean luckPerms   = false;
    private static boolean worldGuard  = false;
    private static boolean placeholderAPI = false;

    public static void init(JavaPlugin plugin) {
        // LuckPerms
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = true;
            plugin.getLogger().info("Hooked into LuckPerms \u2705");
        } else {
            plugin.getLogger().info("LuckPerms not found \u274C — chat will show without prefix.");
        }

        // WorldGuard
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuard = true;
            plugin.getLogger().info("Hooked into WorldGuard \u2705");
        } else {
            plugin.getLogger().info("WorldGuard not found \u274C — combat region blocking disabled.");
        }

        // PlaceholderAPI
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPI = true;
            plugin.getLogger().info("Hooked into PlaceholderAPI \u2705");
        } else {
            plugin.getLogger().info("PlaceholderAPI not found \u274C — PAPI placeholders disabled.");
        }
    }

    public static boolean hasLuckPerms()      { return luckPerms; }
    public static boolean hasWorldGuard()     { return worldGuard; }
    public static boolean hasPlaceholderAPI() { return placeholderAPI; }
}
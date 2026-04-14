package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central hook manager for all soft-dependent plugins.
 * Call BobHooks.init(plugin) once in onEnable() — it checks each plugin,
 * logs a clear success/failure message, and exposes static flags that the
 * rest of Bob uses to guard any calls into those APIs.
 */
public class BobHooks {

    private static boolean vault          = false;
    private static boolean worldGuard     = false;
    private static boolean placeholderAPI = false;
    private static boolean tab            = false;

    public static void init(JavaPlugin plugin) {
        // Vault
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            vault = true;
            plugin.getLogger().info("Hooked into Vault ✅");
        } else {
            plugin.getLogger().info("Vault not found ❌ — chat will show without prefix.");
        }

        // WorldGuard
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuard = true;
            plugin.getLogger().info("Hooked into WorldGuard ✅");
        } else {
            plugin.getLogger().info("WorldGuard not found ❌ — combat region blocking disabled.");
        }

        // PlaceholderAPI
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPI = true;
            plugin.getLogger().info("Hooked into PlaceholderAPI ✅");
        } else {
            plugin.getLogger().info("PlaceholderAPI not found ❌ — PAPI placeholders disabled.");
        }

        // TAB
        if (plugin.getServer().getPluginManager().getPlugin("TAB") != null) {
            tab = true;
            plugin.getLogger().info("Hooked into TAB ✅");
        } else {
            plugin.getLogger().info("TAB not found ❌ — tab list vanish integration disabled.");
        }
    }

    public static boolean hasVault()          { return vault; }
    public static boolean hasWorldGuard()     { return worldGuard; }
    public static boolean hasPlaceholderAPI() { return placeholderAPI; }
    public static boolean hasTab()            { return tab; }
}
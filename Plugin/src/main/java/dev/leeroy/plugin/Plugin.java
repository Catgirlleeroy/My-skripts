package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.commands.*;
import dev.leeroy.plugin.listeners.BanListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    private BanManager banManager;

    @Override
    public void onEnable() {
        // Ban system
        banManager = new BanManager(this);

        // Heal command
        getCommand("heal").setExecutor(new HealCommand());

        // Gamemode shortcut commands
        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);

        // Ban commands
        getCommand("ban").setExecutor(new BanCommand(banManager));
        getCommand("tempban").setExecutor(new TempBanCommand(banManager));
        getCommand("checkban").setExecutor(new CheckBanCommand(banManager));
        getCommand("unban").setExecutor(new UnbanCommand(banManager));

        // Listeners
        getServer().getPluginManager().registerEvents(new BanListener(banManager), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
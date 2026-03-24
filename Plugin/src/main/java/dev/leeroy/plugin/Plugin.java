package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.commands.*;
import dev.leeroy.plugin.listeners.BanListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    private BanManager banManager;

    @Override
    public void onEnable() {
        banManager = new BanManager(this);

        getCommand("heal").setExecutor(new HealCommand());

        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);

        getCommand("ban").setExecutor(new BanCommand(banManager));
        getCommand("tempban").setExecutor(new TempBanCommand(banManager));
        getCommand("checkban").setExecutor(new CheckBanCommand(banManager));

        getServer().getPluginManager().registerEvents(new BanListener(banManager), this);
    }

    @Override
    public void onDisable() {
    }
}
package dev.leeroy.plugin;

import dev.leeroy.plugin.commands.Gamemodes;
import dev.leeroy.plugin.commands.HealCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("heal").setExecutor(new HealCommand());

        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);
    }

    @Override
    public void onDisable() {
    }
}
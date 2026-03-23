package dev.leeroy.plugin;

import dev.leeroy.plugin.commands.Gamemodes;
import dev.leeroy.plugin.commands.HealCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("heal").setExecutor(new HealCommand());
        getCommand("Gamemode").setExecutor(new Gamemodes());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
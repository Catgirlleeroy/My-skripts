package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.commands.*;
import dev.leeroy.plugin.listeners.BanListener;
import dev.leeroy.plugin.listeners.CommandSpyListener;
import dev.leeroy.plugin.listeners.PlayerCacheListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    private BanManager banManager;
    private IPBanManager ipBanManager;
    private PlayerCache playerCache;

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Systems
        banManager   = new BanManager(this);
        ipBanManager = new IPBanManager(this);
        playerCache  = new PlayerCache(this);

        // Heal command
        getCommand("heal").setExecutor(new HealCommand());

        // Gamemode shortcut commands
        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);

        // Ban commands
        getCommand("ban").setExecutor(new BanCommand(banManager, this));
        getCommand("tempban").setExecutor(new TempBanCommand(banManager, this));
        getCommand("checkban").setExecutor(new CheckBanCommand(banManager, playerCache));
        getCommand("unban").setExecutor(new UnbanCommand(banManager, playerCache));

        // IP Ban commands
        getCommand("ipban").setExecutor(new IPBanCommand(ipBanManager, this));
        getCommand("iptempban").setExecutor(new IPTempBanCommand(ipBanManager, this));
        getCommand("ipunban").setExecutor(new IPUnbanCommand(ipBanManager));
        getCommand("checkipban").setExecutor(new CheckIPBanCommand(ipBanManager));

        // Reload command
        getCommand("bobreload").setExecutor(new ReloadCommand(this, banManager, ipBanManager));

        // TP commands
        getCommand("tp").setExecutor(new TPCommand());
        TPACommand tpaCommand = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);
        getServer().getPluginManager().registerEvents(tpaCommand, this);

        // Listeners
        getServer().getPluginManager().registerEvents(new BanListener(banManager, ipBanManager), this);
        getServer().getPluginManager().registerEvents(new PlayerCacheListener(playerCache), this);

        // CommandSpy
        CommandSpyListener commandSpyListener = new CommandSpyListener();
        getServer().getPluginManager().registerEvents(commandSpyListener, this);
        getCommand("commandspy").setExecutor(new CommandSpyCommand(commandSpyListener));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
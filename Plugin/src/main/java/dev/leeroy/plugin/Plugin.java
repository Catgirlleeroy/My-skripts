package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.commands.*;
import dev.leeroy.plugin.listeners.BanListener;
import dev.leeroy.plugin.listeners.CommandSpyListener;
import dev.leeroy.plugin.listeners.MuteListener;
import dev.leeroy.plugin.listeners.PlayerCacheListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    private BanManager banManager;
    private IPBanManager ipBanManager;
    private MuteManager muteManager;
    private PlayerCache playerCache;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Systems
        banManager   = new BanManager(this);
        ipBanManager = new IPBanManager(this);
        muteManager  = new MuteManager(this);
        playerCache  = new PlayerCache(this);

        // Heal
        getCommand("heal").setExecutor(new HealCommand());

        // Gamemode
        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);

        // Ban
        getCommand("ban").setExecutor(new BanCommand(banManager, this));
        getCommand("tempban").setExecutor(new TempBanCommand(banManager, this));
        getCommand("checkban").setExecutor(new CheckBanCommand(banManager, playerCache));
        getCommand("unban").setExecutor(new UnbanCommand(banManager, playerCache));

        // IP Ban
        getCommand("ipban").setExecutor(new IPBanCommand(ipBanManager, this));
        getCommand("iptempban").setExecutor(new IPTempBanCommand(ipBanManager, this));
        getCommand("ipunban").setExecutor(new IPUnbanCommand(ipBanManager));
        getCommand("checkipban").setExecutor(new CheckIPBanCommand(ipBanManager));

        // Mute
        MuteListener muteListener = new MuteListener(muteManager);
        getCommand("mute").setExecutor(new MuteCommand(muteManager));
        getCommand("tempmute").setExecutor(new TempMuteCommand(muteManager));
        getCommand("unmute").setExecutor(new UnmuteCommand(muteManager, playerCache));
        getCommand("chatmute").setExecutor(new ChatMuteCommand(muteListener));
        getCommand("chatclear").setExecutor(new ChatClearCommand());
        getServer().getPluginManager().registerEvents(muteListener, this);

        // Kick
        getCommand("kick").setExecutor(new KickCommand());

        // Reload
        getCommand("bobreload").setExecutor(new ReloadCommand(this, banManager, ipBanManager));

        // TP
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
    public void onDisable() {}
}
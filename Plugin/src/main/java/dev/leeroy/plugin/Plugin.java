package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.Utils.PunishConfig;
import dev.leeroy.plugin.Utils.ReportManager;
import dev.leeroy.plugin.Utils.VanishManager;
import dev.leeroy.plugin.commands.*;
import dev.leeroy.plugin.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {

    private BanManager banManager;
    private IPBanManager ipBanManager;
    private MuteManager muteManager;
    private PlayerCache playerCache;
    private PunishConfig punishConfig;
    private VanishManager vanishManager;
    private ReportManager reportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Systems
        banManager    = new BanManager(this);
        ipBanManager  = new IPBanManager(this);
        muteManager   = new MuteManager(this);
        playerCache   = new PlayerCache(this);
        punishConfig  = new PunishConfig(this);
        vanishManager = new VanishManager();
        reportManager = new ReportManager(this);

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
        getCommand("ipunban").setExecutor(new IPUnbanCommand(ipBanManager, playerCache));
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

        // Punish GUI
        getCommand("punish").setExecutor(new PunishCommand(this, punishConfig));
        getServer().getPluginManager().registerEvents(
                new PunishListener(this, banManager, ipBanManager, muteManager, punishConfig), this);

        // Chat Color
        getCommand("chatcolor").setExecutor(new ChatColorCommand());
        getServer().getPluginManager().registerEvents(new ChatColorListener(), this);

        // Glow
        getCommand("glow").setExecutor(new GlowCommand());
        getServer().getPluginManager().registerEvents(new GlowListener(this), this);

        // Join/Leave messages
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager), this);

        // Report
        getCommand("report").setExecutor(new ReportCommand(reportManager, vanishManager));
        getCommand("reports").setExecutor(new ReportsCommand(reportManager));

        // Reload
        getCommand("bobreload").setExecutor(new ReloadCommand(this, banManager, ipBanManager, punishConfig));

        // TP
        getCommand("tp").setExecutor(new TPCommand());
        TPACommand tpaCommand = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);
        getServer().getPluginManager().registerEvents(tpaCommand, this);

        // Listeners
        getServer().getPluginManager().registerEvents(new BanListener(banManager, ipBanManager, playerCache), this);
        getServer().getPluginManager().registerEvents(new PlayerCacheListener(playerCache), this);

        // CommandSpy
        CommandSpyListener commandSpyListener = new CommandSpyListener();
        getServer().getPluginManager().registerEvents(commandSpyListener, this);
        getCommand("commandspy").setExecutor(new CommandSpyCommand(commandSpyListener));
    }

    @Override
    public void onDisable() {}
}
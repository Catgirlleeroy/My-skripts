package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.AutoMessageManager;
import dev.leeroy.plugin.Utils.BobHooks;
import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.ChatGameManager;
import dev.leeroy.plugin.Utils.CombatManager;
import dev.leeroy.plugin.Utils.DailyRewardManager;
import dev.leeroy.plugin.Utils.FlyConfig;
import dev.leeroy.plugin.Utils.FlyDataManager;
import dev.leeroy.plugin.Utils.FlyManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.Utils.PunishConfig;
import dev.leeroy.plugin.Utils.ReportManager;
import dev.leeroy.plugin.Utils.VanishManager;
import dev.leeroy.plugin.Utils.WarnManager;
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
    private FlyDataManager flyDataManager;
    private FlyConfig flyConfig;
    private FlyManager flyManager;
    private AutoMessageManager autoMessageManager;
    private ChatGameManager chatGameManager;
    private CombatManager combatManager;
    private DailyRewardManager dailyRewardManager;
    private WarnManager warnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Startup banner
        String version = getDescription().getVersion();
        getLogger().info(" ");
        getLogger().info(" ██████╗  ██████╗ ██████╗ ");
        getLogger().info(" ██╔══██╗██╔═══██╗██╔══██╗");
        getLogger().info(" ██████╔╝██║   ██║██████╔╝");
        getLogger().info(" ██╔══██╗██║   ██║██╔══██╗");
        getLogger().info(" ██████╔╝╚██████╔╝██████╔╝");
        getLogger().info(" ╚═════╝  ╚═════╝ ╚═════╝  v" + version);
        getLogger().info(" ");

        // Check and log all soft-dependent plugins
        BobHooks.init(this);

        // Systems
        banManager         = new BanManager(this);
        ipBanManager       = new IPBanManager(this);
        muteManager        = new MuteManager(this);
        playerCache        = new PlayerCache(this);
        punishConfig       = new PunishConfig(this);
        vanishManager      = new VanishManager();
        reportManager      = new ReportManager(this);
        flyDataManager     = new FlyDataManager(this);
        flyConfig          = new FlyConfig(this);
        flyManager         = new FlyManager(this, flyDataManager, flyConfig);
        autoMessageManager = new AutoMessageManager(this);
        chatGameManager    = new ChatGameManager(this);
        combatManager      = new CombatManager(this);
        flyManager.setCombatManager(combatManager);
        dailyRewardManager = new DailyRewardManager(this);
        warnManager        = new WarnManager(this);

        // Heal
        getCommand("heal").setExecutor(new HealCommand());

        // Gamemodes
        Gamemodes gamemodesExecutor = new Gamemodes();
        getCommand("gm0").setExecutor(gamemodesExecutor);
        getCommand("gm1").setExecutor(gamemodesExecutor);
        getCommand("gm2").setExecutor(gamemodesExecutor);
        getCommand("gm3").setExecutor(gamemodesExecutor);

        // Ban / IP Ban
        getCommand("ban").setExecutor(new BanCommand(banManager, playerCache, this));
        getCommand("tempban").setExecutor(new TempBanCommand(banManager, playerCache, this));
        getCommand("unban").setExecutor(new UnbanCommand(banManager, playerCache));
        getCommand("checkban").setExecutor(new CheckBanCommand(banManager, playerCache));
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
                new PunishListener(this, banManager, ipBanManager, muteManager,
                        punishConfig, warnManager, playerCache), this);

        // Warn
        getCommand("warn").setExecutor(new WarnCommand(warnManager, playerCache, this, "warn"));
        getCommand("unwarn").setExecutor(new WarnCommand(warnManager, playerCache, this, "unwarn"));
        getCommand("warns").setExecutor(new WarnCommand(warnManager, playerCache, this, "warns"));

        // Chat Color
        getCommand("chatcolor").setExecutor(new ChatColorCommand());
        getServer().getPluginManager().registerEvents(new ChatColorListener(), this);

        // Chat Format (LuckPerms group prefix)
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);

        // Glow
        getCommand("glow").setExecutor(new GlowCommand());
        getServer().getPluginManager().registerEvents(new GlowListener(this), this);

        // Fly
        getCommand("fly").setExecutor(new FlyCommand(flyManager, flyDataManager, flyConfig, playerCache));
        getServer().getPluginManager().registerEvents(new FlyListener(flyManager, flyDataManager, flyConfig), this);

        // Misc listeners
        getServer().getPluginManager().registerEvents(new FullInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager), this);

        // Report
        getCommand("report").setExecutor(new ReportCommand(reportManager, vanishManager));
        getCommand("reports").setExecutor(new ReportsCommand(reportManager));

        // Reload
        getCommand("bobreload").setExecutor(
                new ReloadCommand(this, banManager, ipBanManager, punishConfig,
                        autoMessageManager, chatGameManager));

        // Daily Reward
        getCommand("daily").setExecutor(new DailyRewardCommand(dailyRewardManager, false));
        getCommand("fixdaily").setExecutor(new DailyRewardCommand(dailyRewardManager, true));
        getServer().getPluginManager().registerEvents(new DailyRewardListener(dailyRewardManager), this);

        // Combat Tag
        getCommand("setcombat").setExecutor(new CombatCommand(combatManager, true));
        getCommand("removecombat").setExecutor(new CombatCommand(combatManager, false));
        getServer().getPluginManager().registerEvents(new CombatListener(combatManager, this), this);

        // Chat Game
        getCommand("chatgame").setExecutor(new ChatGameCommand(chatGameManager, this));
        getServer().getPluginManager().registerEvents(new ChatGameListener(chatGameManager), this);

        // TP
        getCommand("tp").setExecutor(new TPCommand());
        TPACommand tpaCommand = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);
        getServer().getPluginManager().registerEvents(tpaCommand, this);

        // Core listeners
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
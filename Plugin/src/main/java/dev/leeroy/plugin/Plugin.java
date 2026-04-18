package dev.leeroy.plugin;

import dev.leeroy.plugin.Utils.chat.*;
import dev.leeroy.plugin.Utils.combat.*;
import dev.leeroy.plugin.Utils.misc.*;
import dev.leeroy.plugin.Utils.punishment.*;
import dev.leeroy.plugin.commands.chat.*;
import dev.leeroy.plugin.commands.combat.*;
import dev.leeroy.plugin.commands.misc.*;
import dev.leeroy.plugin.commands.punishment.*;
import dev.leeroy.plugin.listeners.chat.*;
import dev.leeroy.plugin.listeners.combat.*;
import dev.leeroy.plugin.listeners.misc.*;
import dev.leeroy.plugin.listeners.punishment.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.GameMode;
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
        vanishManager      = new VanishManager(this);
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

        // Shared listener instances (needed by both commands and event handlers)
        MuteListener       muteListener       = new MuteListener(muteManager);
        CommandSpyListener commandSpyListener = new CommandSpyListener();
        TPACommand         tpaCommand         = new TPACommand(this);

        // Event listeners
        getServer().getPluginManager().registerEvents(muteListener, this);
        getServer().getPluginManager().registerEvents(new PunishListener(this, banManager, ipBanManager,
                muteManager, punishConfig, warnManager, playerCache), this);
        getServer().getPluginManager().registerEvents(new ChatColorListener(), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);
        getServer().getPluginManager().registerEvents(new GlowListener(this), this);
        getServer().getPluginManager().registerEvents(new FlyListener(flyManager, flyDataManager, flyConfig), this);
        getServer().getPluginManager().registerEvents(new FullInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager, this), this);
        getServer().getPluginManager().registerEvents(new DailyRewardListener(dailyRewardManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(combatManager, this), this);
        getServer().getPluginManager().registerEvents(new ChatGameListener(chatGameManager), this);
        getServer().getPluginManager().registerEvents(tpaCommand, this);
        getServer().getPluginManager().registerEvents(new BanListener(banManager, ipBanManager, playerCache), this);
        getServer().getPluginManager().registerEvents(new PlayerCacheListener(playerCache), this);
        getServer().getPluginManager().registerEvents(commandSpyListener, this);

        // Capture finals for command registration lambda
        final BanManager         fBan       = banManager;
        final IPBanManager       fIPBan     = ipBanManager;
        final MuteManager        fMute      = muteManager;
        final PlayerCache        fCache     = playerCache;
        final PunishConfig       fPunishCfg = punishConfig;
        final VanishManager      fVanish    = vanishManager;
        final ReportManager      fReport    = reportManager;
        final FlyDataManager     fFlyData   = flyDataManager;
        final FlyConfig          fFlyCfg    = flyConfig;
        final FlyManager         fFly       = flyManager;
        final AutoMessageManager fAutoMsg   = autoMessageManager;
        final ChatGameManager    fChatGame  = chatGameManager;
        final CombatManager      fCombat    = combatManager;
        final DailyRewardManager fDaily     = dailyRewardManager;
        final WarnManager        fWarn      = warnManager;
        final MuteListener       fMuteLsr   = muteListener;
        final CommandSpyListener fSpyLsr    = commandSpyListener;
        final TPACommand         fTpa       = tpaCommand;

        // Register commands via Paper LifecycleEvents
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands cmds = event.registrar();

            // Heal
            cmds.register("heal", new HealCommand(fVanish));

            // Gamemodes
            cmds.register("gm0", new Gamemodes(GameMode.SURVIVAL,  "gm0"));
            cmds.register("gm1", new Gamemodes(GameMode.CREATIVE,  "gm1"));
            cmds.register("gm2", new Gamemodes(GameMode.ADVENTURE, "gm2"));
            cmds.register("gm3", new Gamemodes(GameMode.SPECTATOR, "gm3"));

            // Ban / IP Ban
            cmds.register("ban",        new BanCommand(fBan, fCache, Plugin.this, fVanish));
            cmds.register("tempban",    new TempBanCommand(fBan, fCache, Plugin.this, fVanish));
            cmds.register("unban",      new UnbanCommand(fBan, fCache));
            cmds.register("checkban",   new CheckBanCommand(fBan, fCache));
            cmds.register("ipban",      new IPBanCommand(fIPBan, Plugin.this));
            cmds.register("iptempban",  new IPTempBanCommand(fIPBan, Plugin.this));
            cmds.register("ipunban",    new IPUnbanCommand(fIPBan, fCache));
            cmds.register("checkipban", new CheckIPBanCommand(fIPBan));

            // Mute
            cmds.register("mute",      new MuteCommand(fMute, fVanish));
            cmds.register("tempmute",  new TempMuteCommand(fMute, fVanish));
            cmds.register("unmute",    new UnmuteCommand(fMute, fCache));
            cmds.register("chatmute",  new ChatMuteCommand(fMuteLsr));
            cmds.register("chatclear", new ChatClearCommand());

            // Kick
            cmds.register("kick", new KickCommand(fVanish));

            // Punish GUI
            cmds.register("punish", new PunishCommand(Plugin.this, fPunishCfg, fVanish));

            // Warn
            cmds.register("warn",   new WarnCommand(fWarn, fCache, Plugin.this, "warn",   fVanish));
            cmds.register("unwarn", new WarnCommand(fWarn, fCache, Plugin.this, "unwarn", fVanish));
            cmds.register("warns",  new WarnCommand(fWarn, fCache, Plugin.this, "warns",  fVanish));

            // Chat Color
            cmds.register("chatcolor", new ChatColorCommand());

            // Glow
            cmds.register("glow", new GlowCommand());

            // Fly
            cmds.register("fly", new FlyCommand(fFly, fFlyData, fFlyCfg, fCache, fVanish));

            // Vanish
            cmds.register("vanish", new VanishCommand(fVanish));

            // Report
            cmds.register("report",  new ReportCommand(fReport, fVanish));
            cmds.register("reports", new ReportsCommand(fReport));

            // Reload
            cmds.register("bobreload", new ReloadCommand(Plugin.this, fBan, fIPBan,
                    fPunishCfg, fAutoMsg, fChatGame));

            // Daily Reward
            cmds.register("daily",    new DailyRewardCommand(fDaily, false));
            cmds.register("fixdaily", new DailyRewardCommand(fDaily, true));

            // Combat Tag
            cmds.register("setcombat",    new CombatCommand(fCombat, true));
            cmds.register("removecombat", new CombatCommand(fCombat, false));

            // Chat Game
            cmds.register("chatgame", new ChatGameCommand(fChatGame, Plugin.this));

            // TP
            cmds.register("tp",       new TPCommand(fVanish));
            cmds.register("tphere",   new TPHereCommand(fVanish));
            cmds.register("tpa",      fTpa.tpa());
            cmds.register("tpaccept", fTpa.tpaccept());
            cmds.register("tpdeny",   fTpa.tpdeny());

            // CommandSpy
            cmds.register("commandspy", new CommandSpyCommand(fSpyLsr));
        });
    }

    @Override
    public void onDisable() {
        playerCache.saveNow();
    }
}

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
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
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
        TPACommand         tpaCommand         = new TPACommand(this, vanishManager);

        // Event listeners
        getServer().getPluginManager().registerEvents(muteListener, this);
        getServer().getPluginManager().registerEvents(new PunishListener(this, banManager, ipBanManager,
                muteManager, punishConfig, warnManager, playerCache), this);
        getServer().getPluginManager().registerEvents(new ChatColorListener(), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);
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

        // DiscordSRV mute suppression — only register if DiscordSRV is loaded
        if (BobHooks.hasDiscordSRV()) {
            github.scarsz.discordsrv.DiscordSRV.api.subscribe(
                    new DiscordSRVChatListener(muteManager, muteListener, chatGameManager));
        }

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
            cmds.register("heal", perm(new HealCommand(fVanish), "bob.heal"));

            // Gamemodes
            cmds.register("gm0", perm(new Gamemodes(GameMode.SURVIVAL,  "gm0"), "bob.gamemode.self"));
            cmds.register("gm1", perm(new Gamemodes(GameMode.CREATIVE,  "gm1"), "bob.gamemode.self"));
            cmds.register("gm2", perm(new Gamemodes(GameMode.ADVENTURE, "gm2"), "bob.gamemode.self"));
            cmds.register("gm3", perm(new Gamemodes(GameMode.SPECTATOR, "gm3"), "bob.gamemode.self"));

            // Ban / IP Ban
            cmds.register("ban",        perm(new BanCommand(fBan, fCache, Plugin.this, fVanish),       "bob.ban"));
            cmds.register("tempban",    perm(new TempBanCommand(fBan, fCache, Plugin.this, fVanish),    "bob.tempban"));
            cmds.register("unban",      perm(new UnbanCommand(fBan, fCache),                            "bob.unban"));
            cmds.register("checkban",   perm(new CheckBanCommand(fBan, fCache),                         "bob.checkban"));
            cmds.register("ipban",      perm(new IPBanCommand(fIPBan, Plugin.this),                     "bob.ipban"));
            cmds.register("iptempban",  perm(new IPTempBanCommand(fIPBan, Plugin.this),                 "bob.iptempban"));
            cmds.register("ipunban",    perm(new IPUnbanCommand(fIPBan, fCache),                        "bob.ipunban"));
            cmds.register("checkipban", perm(new CheckIPBanCommand(fIPBan),                             "bob.checkipban"));

            // Mute
            cmds.register("mute",      perm(new MuteCommand(fMute, fVanish),       "bob.mute"));
            cmds.register("tempmute",  perm(new TempMuteCommand(fMute, fVanish),   "bob.tempmute"));
            cmds.register("unmute",    perm(new UnmuteCommand(fMute, fCache),       "bob.unmute"));
            cmds.register("chatmute",  perm(new ChatMuteCommand(fMuteLsr),          "bob.chatmute"));
            cmds.register("chatclear", perm(new ChatClearCommand(),                 "bob.chatclear"));

            // Kick
            cmds.register("kick", perm(new KickCommand(fVanish), "bob.kick"));

            // Punish GUI
            cmds.register("punish", perm(new PunishCommand(Plugin.this, fPunishCfg, fVanish), "bob.punish"));

            // Warn
            cmds.register("warn",   perm(new WarnCommand(fWarn, fCache, Plugin.this, "warn",   fVanish), "bob.warn"));
            cmds.register("unwarn", perm(new WarnCommand(fWarn, fCache, Plugin.this, "unwarn", fVanish), "bob.warn"));
            cmds.register("warns",  new WarnCommand(fWarn, fCache, Plugin.this, "warns", fVanish));

            // Chat Color
            cmds.register("chatcolor", perm(new ChatColorCommand(), "bob.chatcolor"));

            // Fly
            cmds.register("fly", perm(new FlyCommand(fFly, fFlyData, fFlyCfg, fCache, fVanish), "bob.fly"));

            // Vanish
            cmds.register("vanish", perm(new VanishCommand(fVanish), "bob.vanish"));

            // Report
            cmds.register("report",  perm(new ReportCommand(fReport, fVanish), "bob.report"));
            cmds.register("reports", perm(new ReportsCommand(fReport),          "bob.reports"));

            // Reload
            cmds.register("bobreload", perm(new ReloadCommand(Plugin.this, fBan, fIPBan,
                    fPunishCfg, fAutoMsg, fChatGame), "bob.reload"));

            // Daily Reward
            cmds.register("daily",    new DailyRewardCommand(fDaily, false));
            cmds.register("fixdaily", perm(new DailyRewardCommand(fDaily, true), "bob.daily.admin"));

            // Combat Tag
            cmds.register("setcombat",    perm(new CombatCommand(fCombat, true),  "bob.combat.admin"));
            cmds.register("removecombat", perm(new CombatCommand(fCombat, false), "bob.combat.admin"));

            // Chat Game
            cmds.register("chatgame", perm(new ChatGameCommand(fChatGame, Plugin.this), "bob.chatgame"));

            // TP
            cmds.register("tp",       perm(new TPCommand(fVanish),    "bob.tp.self"));
            cmds.register("tphere",   perm(new TPHereCommand(fVanish), "bob.tp.here"));
            cmds.register("tpa",      perm(fTpa.tpa(),      "bob.tpa"));
            cmds.register("tpaccept", perm(fTpa.tpaccept(), "bob.tpa"));
            cmds.register("tpdeny",   perm(fTpa.tpdeny(),   "bob.tpa"));

            // CommandSpy
            cmds.register("commandspy", perm(new CommandSpyCommand(fSpyLsr), "bob.commandspy"));
        });
    }

    private static BasicCommand perm(BasicCommand cmd, String permission) {
        return new BasicCommand() {
            @Override
            public void execute(CommandSourceStack stack, String[] args) { cmd.execute(stack, args); }
            @Override
            public java.util.Collection<String> suggest(CommandSourceStack stack, String[] args) { return cmd.suggest(stack, args); }
            @Override
            public boolean canUse(org.bukkit.command.CommandSender sender) { return sender.hasPermission(permission); }
        };
    }

    @Override
    public void onDisable() {
        playerCache.saveNow();
    }
}

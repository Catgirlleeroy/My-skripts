package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.MessagesConfig;
import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
import dev.leeroy.plugin.Utils.punishment.PunishmentHistoryManager;
import dev.leeroy.plugin.Utils.punishment.WarnManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class WarnCommand implements BasicCommand {

    private final WarnManager warnManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;
    private final String mode;
    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final MessagesConfig messagesConfig;
    private final PunishmentHistoryManager historyManager;

    public WarnCommand(WarnManager warnManager, PlayerCache playerCache,
                       JavaPlugin plugin, String mode, VanishManager vanishManager,
                       PunishmentDiscordBroadcaster discordBroadcaster, MessagesConfig messagesConfig,
                       PunishmentHistoryManager historyManager) {
        this.warnManager        = warnManager;
        this.playerCache        = playerCache;
        this.plugin             = plugin;
        this.mode               = mode;
        this.vanishManager      = vanishManager;
        this.discordBroadcaster = discordBroadcaster;
        this.messagesConfig     = messagesConfig;
        this.historyManager     = historyManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        switch (mode) {
            case "warn"   -> handleWarn(sender, args);
            case "unwarn" -> handleUnwarn(sender, args);
            case "warns"  -> handleWarns(sender, args);
        }
    }

    private void handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bob.warn")) {
            sender.sendMessage(Component.text("You don't have permission to warn players.", NamedTextColor.RED));
            return;
        }
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /warn <player> [reason]", NamedTextColor.YELLOW)); return; }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(Component.text("Player '" + args[0] + "' not found.", NamedTextColor.RED)); return; }

        String targetName = getName(uuid, args[0]);
        String reason     = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        int max           = plugin.getConfig().getInt("warn.max-warns", 3);
        int warns         = warnManager.addWarn(uuid);
        historyManager.log(uuid, targetName, "warn", reason != null ? reason : "No reason", sender.getName(), null);

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            online.playSound(online.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        }

        String broadcastKey = reason != null ? "warned-broadcast" : "warned-broadcast-no-reason";
        String msg = messagesConfig.get().getString("warn.messages." + broadcastKey, "")
                .replace("{player}", targetName)
                .replace("{staff}",  sender.getName())
                .replace("{reason}", reason != null ? reason : "")
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max));

        Bukkit.broadcast(Component.empty());
        TextUtil.broadcast("&4&l                  WARNING!");
        Bukkit.broadcast(Component.empty());
        TextUtil.broadcast(msg);
        Bukkit.broadcast(Component.empty());
        discordBroadcaster.broadcastWarn("warn", targetName, sender.getName(), reason, warns, max);

        // Personal message to the warned player
        if (online != null) {
            if (warns >= max) {
                String maxMsg = messagesConfig.get().getString("warn.messages.personal-warn-max",
                                "&cYou have reached &4{max}&c warnings and will be punished!")
                        .replace("{warns}", String.valueOf(warns))
                        .replace("{max}",   String.valueOf(max));
                online.sendMessage(TextUtil.parse(maxMsg));
            } else {
                List<String> punishments = plugin.getConfig().getStringList("warn.stacked-punishments");
                int offense = warnManager.getOffenses(uuid);
                int offenseDisplay = offense + 1;
                String rawCmd = punishments.isEmpty()
                        ? "tempban {player} 1d"
                        : punishments.get(Math.min(offense, punishments.size() - 1));
                String personalMsg = messagesConfig.get().getString("warn.messages.personal-warn",
                                "&7You now have &4{warns}&8/&4{max} &7warnings. &cYou will be &4{punishment} &cif you get too many warnings &8(Offense &4{offense}&8)")
                        .replace("{warns}",      String.valueOf(warns))
                        .replace("{max}",        String.valueOf(max))
                        .replace("{punishment}", describePunishment(rawCmd))
                        .replace("{offense}",    String.valueOf(offenseDisplay));
                online.sendMessage(TextUtil.parse(personalMsg));
            }
        }

        if (warns >= max) {
            final UUID fUUID = uuid;
            final String fName = targetName;
            final int offense = warnManager.getOffenses(uuid);
            warnManager.incrementOffenses(uuid);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (warnManager.getWarns(fUUID) >= max) {
                    if (!fName.matches("[a-zA-Z0-9_]{1,16}")) return;
                    java.util.List<String> punishments = plugin.getConfig()
                            .getStringList("warn.stacked-punishments");
                    String punishCmd;
                    if (punishments.isEmpty()) {
                        punishCmd = "tempban " + fName + " 1d Too many warnings!";
                    } else {
                        int index = Math.min(offense, punishments.size() - 1);
                        punishCmd = punishments.get(index).replace("{player}", fName);
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCmd);
                    if (plugin.getConfig().getBoolean("warn.reset-on-max", true)) {
                        warnManager.resetWarns(fUUID);
                    }
                }
            }, 40L);
        }
    }

    private void handleUnwarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bob.warn")) {
            sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /unwarn <player>", NamedTextColor.YELLOW)); return; }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(Component.text("Player '" + args[0] + "' not found.", NamedTextColor.RED)); return; }

        String targetName = getName(uuid, args[0]);
        int max           = plugin.getConfig().getInt("warn.max-warns", 3);

        if (warnManager.getWarns(uuid) <= 0) {
            sender.sendMessage(TextUtil.parse(plugin.getConfig()
                    .getString("warn.messages.no-warns", "&4{player} &chas no warns.")
                    .replace("{player}", targetName)));
            return;
        }

        int warns = warnManager.removeWarn(uuid);

        String msg = messagesConfig.get().getString("warn.messages.unwarned-broadcast", "")
                .replace("{player}", targetName)
                .replace("{staff}",  sender.getName())
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max));

        Bukkit.broadcast(Component.empty());
        TextUtil.broadcast("&2&l                  UN-WARN!");
        Bukkit.broadcast(Component.empty());
        TextUtil.broadcast(msg);
        Bukkit.broadcast(Component.empty());
        discordBroadcaster.broadcastWarn("unwarn", targetName, sender.getName(), null, warns, max);
    }

    private void handleWarns(CommandSender sender, String[] args) {
        int max = plugin.getConfig().getInt("warn.max-warns", 3);

        if (args.length == 0 || !(sender instanceof Player)) {
            UUID uuid = sender instanceof Player p ? p.getUniqueId() : null;
            if (uuid == null) { sender.sendMessage(Component.text("Usage: /warns <player>", NamedTextColor.YELLOW)); return; }
            int warns = warnManager.getWarns(uuid);
            sender.sendMessage(TextUtil.parse(messagesConfig.get().getString("warn.messages.check-self", "")
                    .replace("{warns}", String.valueOf(warns))
                    .replace("{max}",   String.valueOf(max))));
            return;
        }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(Component.text("Player '" + args[0] + "' not found.", NamedTextColor.RED)); return; }

        if (sender instanceof Player p && p.getUniqueId().equals(uuid)) {
            sender.sendMessage(TextUtil.parse(messagesConfig.get().getString("warn.messages.check-self", "")
                    .replace("{warns}", String.valueOf(warnManager.getWarns(uuid)))
                    .replace("{max}",   String.valueOf(max))));
            return;
        }

        String targetName = getName(uuid, args[0]);
        int warns = warnManager.getWarns(uuid);
        sender.sendMessage(TextUtil.parse(messagesConfig.get().getString("warn.messages.check-other", "")
                .replace("{player}", targetName)
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max))));
    }

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return playerCache.getUUID(input);
    }

    private String getName(UUID uuid, String fallback) {
        String cached = playerCache.getName(uuid);
        return cached != null ? cached : fallback;
    }

    private String describePunishment(String cmd) {
        String[] parts = cmd.split("\\s+");
        if (parts.length == 0) return cmd;
        return switch (parts[0].toLowerCase()) {
            case "tempban", "iptempban" -> "tempbanned for " + (parts.length > 2 ? parts[2] : "?");
            case "ban",     "ipban"     -> "permanently banned";
            case "tempmute"             -> "muted for "      + (parts.length > 2 ? parts[2] : "?");
            case "mute"                 -> "permanently muted";
            case "kick"                 -> "kicked";
            default                    -> parts[0];
        };
    }
}

package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class TempBanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;

    public TempBanCommand(BanManager banManager, PlayerCache playerCache, JavaPlugin plugin) {
        this.banManager  = banManager;
        this.playerCache = playerCache;
        this.plugin      = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.tempban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to tempban players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tempban <player|uuid> <duration> [reason]");
            sender.sendMessage(ChatColor.GRAY + "Duration examples: 30m, 2h, 1d, 1d12h");
            return true;
        }

        String input       = args[0];
        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily banned.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(ChatColor.RED + "Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.");
            return true;
        }

        // Resolve UUID and name
        UUID   uuid;
        String targetName;
        Player onlineTarget = Bukkit.getPlayerExact(input);

        if (onlineTarget != null) {
            uuid       = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            uuid = tryParseUUID(input);
            if (uuid == null) uuid = playerCache.getUUID(input);
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + input + "' not found. They must have joined before, or provide a UUID.");
                return true;
            }
            String cached = playerCache.getName(uuid);
            targetName = cached != null ? cached : uuid.toString();
        }


        // Check if target is exempt from this punishment
        if (onlineTarget != null && (onlineTarget.hasPermission("bob.exempt") || onlineTarget.hasPermission("bob.exempt.tempban"))) {
            sender.sendMessage(ChatColor.RED + onlineTarget.getName() + " is exempt from this punishment.");
            return true;
        }

        if (banManager.isBanned(uuid)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned.");
            return true;
        }

        banManager.tempBan(uuid, targetName, reason, sender.getName(), durationMs);

        // Effects and kick if online
        if (onlineTarget != null) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
            );
            final String finalReason   = reason;
            final long   finalDuration = durationMs;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    onlineTarget.kickPlayer(
                            ChatColor.RED + "You have been temporarily banned.\n" +
                                    ChatColor.WHITE + "Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + finalDuration) + "\n" +
                                    ChatColor.WHITE + "Reason: " + finalReason
                    ), 10L);
        }

        Bukkit.broadcastMessage(
                ChatColor.RED + "[TEMPBAN] " +
                        ChatColor.YELLOW + targetName +
                        ChatColor.RED + " has been temporarily banned! " +
                        ChatColor.GRAY + "Duration: " + durationStr + " | Reason: " + reason
        );

        sender.sendMessage(ChatColor.GREEN + "Tempbanned " + targetName + " for " + durationStr + ". Reason: " + reason);
        return true;
    }

    private UUID tryParseUUID(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException e) { return null; }
    }
}
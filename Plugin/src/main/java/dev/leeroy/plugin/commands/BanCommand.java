package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.PlayerCache;
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

public class BanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;

    public BanCommand(BanManager banManager, PlayerCache playerCache, JavaPlugin plugin) {
        this.banManager  = banManager;
        this.playerCache = playerCache;
        this.plugin      = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.ban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to ban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ban <player|uuid> [reason]");
            return true;
        }

        String input  = args[0];
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "You have been banned.";

        // Resolve UUID and name — online first, then cache, then raw UUID
        UUID   uuid;
        String targetName;
        Player onlineTarget = Bukkit.getPlayerExact(input);

        if (onlineTarget != null) {
            uuid       = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            // Try raw UUID
            uuid = tryParseUUID(input);
            if (uuid == null) {
                // Try cache by name
                uuid = playerCache.getUUID(input);
            }
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + input + "' not found. They must have joined before, or provide a UUID.");
                return true;
            }
            // Get display name from cache
            String cached = playerCache.getName(uuid);
            targetName = cached != null ? cached : uuid.toString();
        }

        if (banManager.isBanned(uuid)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned.");
            return true;
        }

        banManager.ban(uuid, targetName, reason, sender.getName());

        // Effects and kick if online
        if (onlineTarget != null) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
            );
            final String finalReason = reason;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    onlineTarget.kickPlayer(
                            ChatColor.RED + "You have been permanently banned.\n" +
                                    ChatColor.WHITE + "Reason: " + finalReason
                    ), 10L);
        }

        Bukkit.broadcastMessage(
                ChatColor.RED + "[BAN] " +
                        ChatColor.YELLOW + targetName +
                        ChatColor.RED + " has been permanently banned! " +
                        ChatColor.GRAY + "Reason: " + reason
        );

        sender.sendMessage(ChatColor.GREEN + "Banned " + targetName + ". Reason: " + reason);
        return true;
    }

    private UUID tryParseUUID(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException e) { return null; }
    }
}
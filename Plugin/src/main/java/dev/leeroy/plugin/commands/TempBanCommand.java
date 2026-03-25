package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TempBanCommand implements CommandExecutor {

    private final BanManager banManager;

    public TempBanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bansystem.tempban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to tempban players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tempban <player> <duration> [reason]");
            sender.sendMessage(ChatColor.GRAY  + "Duration examples: 30m, 2h, 1d, 1d12h");
            return true;
        }

        String targetName  = args[0];
        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily banned.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(ChatColor.RED + "Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.");
            return true;
        }

        if (banManager.isBanned(targetName)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned.");
            return true;
        }

        banManager.tempBan(targetName, reason, sender.getName(), durationMs);

        // Effects & kick if online
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            // Lightning strike at their location
            target.getWorld().strikeLightning(target.getLocation());

            // Wither death sound heard by everyone on the server
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f)
            );

            target.kickPlayer(
                    ChatColor.RED + "You have been temporarily banned.\n" +
                            ChatColor.WHITE + "Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + durationMs) + "\n" +
                            ChatColor.WHITE + "Reason: " + reason
            );
        }

        // Public broadcast to all players
        Bukkit.broadcastMessage(
                ChatColor.RED + "[TEMPBAN] " +
                        ChatColor.YELLOW + targetName +
                        ChatColor.RED + " has been temporarily banned! " +
                        ChatColor.GRAY + "Duration: " + durationStr + " | Reason: " + reason
        );

        return true;
    }
}
package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanCommand implements CommandExecutor {

    private final BanManager banManager;

    public BanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bansystem.ban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to ban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ban <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been banned.";

        if (banManager.isBanned(targetName)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned.");
            return true;
        }

        banManager.ban(targetName, reason, sender.getName());

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
                    ChatColor.RED + "You have been permanently banned.\n" +
                            ChatColor.WHITE + "Reason: " + reason
            );
        }

        // Public broadcast to all players
        Bukkit.broadcastMessage(
                ChatColor.RED + "[BAN] " +
                        ChatColor.YELLOW + targetName +
                        ChatColor.RED + " has been permanently banned! " +
                        ChatColor.GRAY + "Reason: " + reason
        );

        return true;
    }
}
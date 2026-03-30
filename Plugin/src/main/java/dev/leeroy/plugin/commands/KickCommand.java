package dev.leeroy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.kick")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to kick players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /kick <player> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }


        // Check if target is exempt from this punishment
        if (target != null && (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.kick"))) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is exempt from this punishment.");
            return true;
        }

        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been kicked.";

        target.kickPlayer(ChatColor.RED + "You have been kicked.\n" + ChatColor.WHITE + "Reason: " + reason);

        Bukkit.broadcastMessage(ChatColor.RED + "[KICK] " + ChatColor.YELLOW + target.getName()
                + ChatColor.RED + " has been kicked! " + ChatColor.GRAY + "Reason: " + reason);
        return true;
    }
}
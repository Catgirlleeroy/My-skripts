package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand implements CommandExecutor {

    private final MuteManager muteManager;

    public MuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.mute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to mute players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mute <player> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been muted.";

        if (muteManager.isMuted(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already muted.");
            return true;
        }

        muteManager.mute(target.getUniqueId(), target.getName(), reason, sender.getName());
        target.sendMessage(ChatColor.RED + "You have been permanently muted. Reason: " + reason);
        Bukkit.broadcastMessage(ChatColor.RED + "[MUTE] " + ChatColor.YELLOW + target.getName()
                + ChatColor.RED + " has been muted! " + ChatColor.GRAY + "Reason: " + reason);
        return true;
    }
}
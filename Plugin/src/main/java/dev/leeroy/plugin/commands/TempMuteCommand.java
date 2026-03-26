package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TempMuteCommand implements CommandExecutor {

    private final MuteManager muteManager;

    public TempMuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.tempmute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to tempmute players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tempmute <player> <duration> [reason]");
            sender.sendMessage(ChatColor.GRAY + "Duration examples: 30m, 2h, 1d");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily muted.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(ChatColor.RED + "Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.");
            return true;
        }

        if (muteManager.isMuted(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already muted.");
            return true;
        }

        muteManager.tempMute(target.getUniqueId(), target.getName(), reason, sender.getName(), durationMs);
        target.sendMessage(ChatColor.RED + "You have been muted for " + durationStr + ". Reason: " + reason);
        Bukkit.broadcastMessage(ChatColor.RED + "[TEMPMUTE] " + ChatColor.YELLOW + target.getName()
                + ChatColor.RED + " has been muted! " + ChatColor.GRAY + "Duration: " + durationStr
                + " | Reason: " + reason);
        return true;
    }
}
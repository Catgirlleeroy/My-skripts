package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnbanCommand implements CommandExecutor {

    private final BanManager banManager;

    public UnbanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.unban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to unban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /unban <player>");
            return true;
        }

        String targetName = args[0];

        if (!banManager.isBanned(targetName)) {
            sender.sendMessage(ChatColor.RED + targetName + " is not currently banned.");
            return true;
        }

        banManager.unban(targetName);
        sender.sendMessage(ChatColor.GREEN + targetName + " has been unbanned.");
        return true;
    }
}
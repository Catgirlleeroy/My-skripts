package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class CheckBanCommand implements CommandExecutor {

    private final BanManager banManager;

    public CheckBanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.checkban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to check bans.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /checkban <player>");
            return true;
        }

        String targetName = args[0];
        Map<String, Object> details = banManager.getBanDetails(targetName);

        if (details == null) {
            sender.sendMessage(ChatColor.GREEN + targetName + " is not currently banned.");
            return true;
        }

        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        sender.sendMessage(ChatColor.GOLD + "━━━ Ban Info: " + targetName + " ━━━");
        sender.sendMessage(ChatColor.YELLOW + "Type: "      + ChatColor.WHITE + (type.equals("permanent") ? "Permanent" : "Temporary"));
        sender.sendMessage(ChatColor.YELLOW + "Reason: "    + ChatColor.WHITE + reason);
        sender.sendMessage(ChatColor.YELLOW + "Banned by: " + ChatColor.WHITE + bannedBy);

        if (expiry != -1L) {
            sender.sendMessage(ChatColor.YELLOW + "Expires in: " + ChatColor.WHITE + BanManager.formatRemaining(expiry));
        }

        return true;
    }
}
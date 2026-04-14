package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CheckIPBanCommand implements CommandExecutor {

    private final IPBanManager ipBanManager;

    public CheckIPBanCommand(IPBanManager ipBanManager) {
        this.ipBanManager = ipBanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.checkipban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to check IP bans.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /checkipban <player|ip>");
            return true;
        }

        // Resolve to IP — check if it's an online player name first, otherwise treat as raw IP
        String input = args[0];
        String ip;

        Player target = Bukkit.getPlayerExact(input);
        if (target != null) {
            ip = target.getAddress().getAddress().getHostAddress();
        } else {
            ip = input;
        }

        Map<String, Object> details = ipBanManager.getBanDetails(ip);

        if (details == null) {
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " is not currently banned.");
            return true;
        }

        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        sender.sendMessage(ChatColor.GOLD + "━━━ IP Ban Info: " + ip + " ━━━");
        sender.sendMessage(ChatColor.YELLOW + "Type: "      + ChatColor.WHITE + (type.equals("permanent") ? "Permanent" : "Temporary"));
        sender.sendMessage(ChatColor.YELLOW + "Reason: "    + ChatColor.WHITE + reason);
        sender.sendMessage(ChatColor.YELLOW + "Banned by: " + ChatColor.WHITE + bannedBy);

        if (expiry != -1L) {
            sender.sendMessage(ChatColor.YELLOW + "Expires in: " + ChatColor.WHITE + BanManager.formatRemaining(expiry));
        }

        return true;
    }
}

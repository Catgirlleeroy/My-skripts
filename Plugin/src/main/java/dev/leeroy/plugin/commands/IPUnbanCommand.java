package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.IPBanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IPUnbanCommand implements CommandExecutor {

    private final IPBanManager ipBanManager;

    public IPUnbanCommand(IPBanManager ipBanManager) {
        this.ipBanManager = ipBanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.ipunban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to unban IPs.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ipunban <player|uuid|ip>");
            return true;
        }

        String input = args[0];
        String ip = resolveToIP(input);

        if (ip == null) {
            sender.sendMessage(ChatColor.RED + "Could not resolve '" + input + "' to an IP. " +
                    "Player must be online, or provide a raw IP.");
            return true;
        }

        if (!ipBanManager.isBanned(ip)) {
            sender.sendMessage(ChatColor.RED + "IP " + ip + " is not currently banned.");
            return true;
        }

        ipBanManager.unban(ip);
        sender.sendMessage(ChatColor.GREEN + "IP " + ip + " (" + input + ") has been unbanned.");
        return true;
    }

    /**
     * Tries to resolve the input to an IP address.
     * Priority: online player name → online player UUID → raw IP string
     */
    private String resolveToIP(String input) {
        // Check online players by name
        Player byName = Bukkit.getPlayerExact(input);
        if (byName != null) {
            return byName.getAddress().getAddress().getHostAddress();
        }

        // Check online players by UUID
        try {
            java.util.UUID uuid = java.util.UUID.fromString(input);
            Player byUUID = Bukkit.getPlayer(uuid);
            if (byUUID != null) {
                return byUUID.getAddress().getAddress().getHostAddress();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, that's fine
        }

        // Treat as raw IP — basic validation
        if (input.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return input;
        }

        return null;
    }
}
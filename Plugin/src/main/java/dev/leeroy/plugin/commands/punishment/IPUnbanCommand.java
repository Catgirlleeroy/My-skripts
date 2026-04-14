package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import dev.leeroy.plugin.Utils.misc.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IPUnbanCommand implements CommandExecutor {

    private final IPBanManager ipBanManager;
    private final PlayerCache playerCache;

    public IPUnbanCommand(IPBanManager ipBanManager, PlayerCache playerCache) {
        this.ipBanManager = ipBanManager;
        this.playerCache = playerCache;
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
            sender.sendMessage(ChatColor.RED + "Could not resolve '" + input + "' to an IP. "
                    + "Player must be online, in cache, or provide a raw IP.");
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
     * Priority: online player name → cached player name → online UUID → raw IP
     */
    private String resolveToIP(String input) {
        // Online by name
        Player byName = Bukkit.getPlayerExact(input);
        if (byName != null) return byName.getAddress().getAddress().getHostAddress();

        // Cached name → UUID → cached IP (works offline!)
        String cachedIP = playerCache.getIPByName(input);
        if (cachedIP != null) return cachedIP;

        // Online by UUID
        try {
            UUID uuid = UUID.fromString(input);
            Player byUUID = Bukkit.getPlayer(uuid);
            if (byUUID != null) return byUUID.getAddress().getAddress().getHostAddress();
            // Also try cached IP by UUID
            String ipByUUID = playerCache.getIP(uuid);
            if (ipByUUID != null) return ipByUUID;
        } catch (IllegalArgumentException ignored) {}

        // Raw IP
        if (input.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) return input;

        return null;
    }
}

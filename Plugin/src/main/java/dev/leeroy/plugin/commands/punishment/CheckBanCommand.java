package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CheckBanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final PlayerCache playerCache;

    public CheckBanCommand(BanManager banManager, PlayerCache playerCache) {
        this.banManager = banManager;
        this.playerCache = playerCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.checkban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to check bans.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /checkban <player|uuid>");
            return true;
        }

        UUID uuid = resolveUUID(args[0]);

        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.");
            return true;
        }

        Map<String, Object> details = banManager.getBanDetails(uuid);

        if (details == null) {
            String name = playerCache.getName(uuid);
            sender.sendMessage(ChatColor.GREEN + (name != null ? name : args[0]) + " is not currently banned.");
            return true;
        }

        String name     = (String) details.get("name");
        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        sender.sendMessage(ChatColor.GOLD + "━━━ Ban Info: " + name + " ━━━");
        sender.sendMessage(ChatColor.YELLOW + "UUID: "      + ChatColor.WHITE + uuid);
        sender.sendMessage(ChatColor.YELLOW + "Type: "      + ChatColor.WHITE + (type.equals("permanent") ? "Permanent" : "Temporary"));
        sender.sendMessage(ChatColor.YELLOW + "Reason: "    + ChatColor.WHITE + reason);
        sender.sendMessage(ChatColor.YELLOW + "Banned by: " + ChatColor.WHITE + bannedBy);

        if (expiry != -1L) {
            sender.sendMessage(ChatColor.YELLOW + "Expires in: " + ChatColor.WHITE + BanManager.formatRemaining(expiry));
        }

        return true;
    }

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();

        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}

        return playerCache.getUUID(input);
    }
}

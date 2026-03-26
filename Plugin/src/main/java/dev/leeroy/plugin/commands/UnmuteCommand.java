package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {

    private final MuteManager muteManager;
    private final PlayerCache playerCache;

    public UnmuteCommand(MuteManager muteManager, PlayerCache playerCache) {
        this.muteManager = muteManager;
        this.playerCache = playerCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.unmute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to unmute players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /unmute <player|uuid>");
            return true;
        }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.");
            return true;
        }

        if (!muteManager.isMuted(uuid)) {
            sender.sendMessage(ChatColor.RED + args[0] + " is not currently muted.");
            return true;
        }

        muteManager.unmute(uuid);
        String name = playerCache.getName(uuid);
        String displayName = name != null ? name : uuid.toString();

        sender.sendMessage(ChatColor.GREEN + displayName + " has been unmuted.");

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) target.sendMessage(ChatColor.GREEN + "You have been unmuted.");
        return true;
    }

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return playerCache.getUUID(input);
    }
}
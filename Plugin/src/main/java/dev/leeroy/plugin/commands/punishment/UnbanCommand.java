package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.PlayerCache;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnbanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final PlayerCache playerCache;

    public UnbanCommand(BanManager banManager, PlayerCache playerCache) {
        this.banManager = banManager;
        this.playerCache = playerCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.unban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to unban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /unban <player|uuid>");
            return true;
        }

        UUID uuid = resolveUUID(args[0]);

        if (uuid == null) {
            sender.sendMessage(ChatColor.RED + "Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.");
            return true;
        }

        if (!banManager.isBanned(uuid)) {
            sender.sendMessage(ChatColor.RED + args[0] + " is not currently banned.");
            return true;
        }

        banManager.unban(uuid);
        String name = playerCache.getName(uuid);
        sender.sendMessage(ChatColor.GREEN + (name != null ? name : uuid.toString()) + " has been unbanned.");
        return true;
    }

    private UUID resolveUUID(String input) {
        // Online player by name
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();

        // Raw UUID string
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}

        // Offline lookup via cache by name
        return playerCache.getUUID(input);
    }
}

package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final JavaPlugin plugin;

    public BanCommand(BanManager banManager, JavaPlugin plugin) {
        this.banManager = banManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.ban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to ban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ban <player> [reason]");
            return true;
        }

        // Player must be online to get their UUID
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been banned.";

        if (banManager.isBanned(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already banned.");
            return true;
        }

        banManager.ban(target.getUniqueId(), target.getName(), reason, sender.getName());

        // Effects
        target.getWorld().strikeLightningEffect(target.getLocation());
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
        );

        // Kick after 10 ticks
        final String finalReason = reason;
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                target.kickPlayer(
                        ChatColor.RED + "You have been permanently banned.\n" +
                                ChatColor.WHITE + "Reason: " + finalReason
                ), 10L);

        Bukkit.broadcastMessage(
                ChatColor.RED + "[BAN] " +
                        ChatColor.YELLOW + target.getName() +
                        ChatColor.RED + " has been permanently banned! " +
                        ChatColor.GRAY + "Reason: " + reason
        );

        return true;
    }
}
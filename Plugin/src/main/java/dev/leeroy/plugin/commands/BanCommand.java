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

        String targetName = args[0];
        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been banned.";

        if (banManager.isBanned(targetName)) {
            sender.sendMessage(ChatColor.RED + targetName + " is already banned.");
            return true;
        }

        banManager.ban(targetName, reason, sender.getName());

        // Effects & kick if online
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            // Lightning and sound immediately
            target.getWorld().strikeLightning(target.getLocation());
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f)
            );

            // Kick after 10 ticks so the lightning renders first
            final String finalReason = reason;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    target.kickPlayer(
                            ChatColor.RED + "You have been permanently banned.\n" +
                                    ChatColor.WHITE + "Reason: " + finalReason
                    ), 10L);
        }

        // Public broadcast to all players
        Bukkit.broadcastMessage(
                ChatColor.RED + "[BAN] " +
                        ChatColor.YELLOW + targetName +
                        ChatColor.RED + " has been permanently banned! " +
                        ChatColor.GRAY + "Reason: " + reason
        );

        return true;
    }
}
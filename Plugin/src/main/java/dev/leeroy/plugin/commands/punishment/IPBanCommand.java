package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class IPBanCommand implements CommandExecutor {

    private final IPBanManager ipBanManager;
    private final JavaPlugin plugin;

    public IPBanCommand(IPBanManager ipBanManager, JavaPlugin plugin) {
        this.ipBanManager = ipBanManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.ipban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to IP ban players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /ipban <player> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' must be online to IP ban.");
            return true;
        }

        String ip = target.getAddress().getAddress().getHostAddress();
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "You have been IP banned.";

        // Check if target is exempt
        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.ipban")) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is exempt from this punishment.");
            return true;
        }

        if (ipBanManager.isBanned(ip)) {
            sender.sendMessage(ChatColor.RED + target.getName() + "'s IP is already banned.");
            return true;
        }

        ipBanManager.ban(ip, reason, sender.getName());

        // Find ALL online players sharing this IP and kick them all
        final String finalReason = reason;
        final String finalIp = ip;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                online.getWorld().strikeLightningEffect(online.getLocation());
            }
        }

        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                    online.kickPlayer(
                            ChatColor.RED + "You have been permanently IP banned.\n" +
                                    ChatColor.WHITE + "Reason: " + finalReason
                    );
                }
            }
        }, 10L);

        // Broadcast
        Bukkit.broadcastMessage(
                ChatColor.RED + "[IP-BAN] " +
                        ChatColor.YELLOW + target.getName() +
                        ChatColor.RED + " has been permanently IP banned! " +
                        ChatColor.GRAY + "Reason: " + reason
        );

        return true;
    }
}

package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class IPTempBanCommand implements CommandExecutor {

    private final IPBanManager ipBanManager;
    private final JavaPlugin plugin;

    public IPTempBanCommand(IPBanManager ipBanManager, JavaPlugin plugin) {
        this.ipBanManager = ipBanManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.iptempban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to IP tempban players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /iptempban <player> <duration> [reason]");
            sender.sendMessage(ChatColor.GRAY + "Duration examples: 30m, 2h, 1d, 1d12h");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' must be online to IP tempban.");
            return true;
        }

        String ip = target.getAddress().getAddress().getHostAddress();
        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily IP banned.";

        // Check if target is exempt
        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.iptempban")) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is exempt from this punishment.");
            return true;
        }

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(ChatColor.RED + "Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.");
            return true;
        }

        if (ipBanManager.isBanned(ip)) {
            sender.sendMessage(ChatColor.RED + target.getName() + "'s IP is already banned.");
            return true;
        }

        ipBanManager.tempBan(ip, reason, sender.getName(), durationMs);

        // Find ALL online players sharing this IP and kick them all
        final String finalReason = reason;
        final long finalDurationMs = durationMs;
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
                            ChatColor.RED + "You have been temporarily IP banned.\n" +
                                    ChatColor.WHITE + "Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + finalDurationMs) + "\n" +
                                    ChatColor.WHITE + "Reason: " + finalReason
                    );
                }
            }
        }, 10L);

        // Broadcast
        Bukkit.broadcastMessage(
                ChatColor.RED + "[IP-TEMPBAN] " +
                        ChatColor.YELLOW + target.getName() +
                        ChatColor.RED + " has been temporarily IP banned! " +
                        ChatColor.GRAY + "Duration: " + durationStr + " | Reason: " + reason
        );

        return true;
    }
}
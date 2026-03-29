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

import java.util.Arrays;

public class TempBanCommand implements CommandExecutor {

    private final BanManager banManager;
    private final JavaPlugin plugin;

    public TempBanCommand(BanManager banManager, JavaPlugin plugin) {
        this.banManager = banManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.tempban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to tempban players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tempban <player> <duration> [reason]");
            sender.sendMessage(ChatColor.GRAY + "Duration examples: 30m, 2h, 1d, 1d12h");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily banned.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(ChatColor.RED + "Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.");
            return true;
        }

        if (banManager.isBanned(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already banned.");
            return true;
        }

        banManager.tempBan(target.getUniqueId(), target.getName(), reason, sender.getName(), durationMs);

        // Effects
        target.getWorld().strikeLightningEffect(target.getLocation());
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
        );

        final String finalReason = reason;
        final long finalDurationMs = durationMs;
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                target.kickPlayer(
                        ChatColor.RED + "You have been temporarily banned.\n" +
                                ChatColor.WHITE + "Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + finalDurationMs) + "\n" +
                                ChatColor.WHITE + "Reason: " + finalReason
                ), 10L);

        Bukkit.broadcastMessage(
                ChatColor.RED + "[TEMPBAN] " +
                        ChatColor.YELLOW + target.getName() +
                        ChatColor.RED + " has been temporarily banned! " +
                        ChatColor.GRAY + "Duration: " + durationStr + " | Reason: " + reason
        );

        return true;
    }
}
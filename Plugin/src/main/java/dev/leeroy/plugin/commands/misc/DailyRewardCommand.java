package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.DailyRewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DailyRewardCommand implements CommandExecutor {

    private final DailyRewardManager dailyManager;
    private final boolean fixMode;

    public DailyRewardCommand(DailyRewardManager dailyManager, boolean fixMode) {
        this.dailyManager = dailyManager;
        this.fixMode      = fixMode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /fixdaily
        if (fixMode) {
            if (!sender.hasPermission("bob.daily.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            dailyManager.resetAll();
            sender.sendMessage(color(dailyManager.plugin().getConfig()
                    .getString("daily-reward.messages.reset-all", "&aReset all daily rewards.")));
            return true;
        }

        // /daily
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!dailyManager.plugin().getConfig().getBoolean("daily-reward.enabled", true)) {
            player.sendMessage(ChatColor.RED + "Daily rewards are currently disabled.");
            return true;
        }

        if (dailyManager.canClaim(player.getUniqueId())) {
            dailyManager.recordClaim(player.getUniqueId());

            String playerName = player.getName();
            if (!playerName.matches("[a-zA-Z0-9_]{1,16}")) {
                player.sendMessage(ChatColor.RED + "Your username contains invalid characters.");
                return true;
            }

            List<String> rewards = dailyManager.plugin().getConfig().getStringList("daily-reward.rewards");
            for (String cmd : rewards) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
            }

            player.sendMessage(color(dailyManager.plugin().getConfig()
                    .getString("daily-reward.messages.claimed", "&eYou claimed your daily reward!")));
        } else {
            int[] remaining = dailyManager.getTimeRemaining(player.getUniqueId());
            int hours   = remaining[0];
            int minutes = remaining[1];

            String timeLeft = hours > 0
                    ? hours + "h " + minutes + "m"
                    : minutes + "m";

            String msg = dailyManager.plugin().getConfig()
                    .getString("daily-reward.messages.cooldown",
                            "&4&lYou need to wait &e{time} &4&lbefore claiming again.")
                    .replace("{hours}",   String.valueOf(hours))
                    .replace("{minutes}", String.valueOf(minutes))
                    .replace("{time}",    timeLeft);

            player.sendMessage(color(msg));
        }

        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.DailyRewardManager;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DailyRewardCommand implements BasicCommand {

    private final DailyRewardManager dailyManager;
    private final boolean fixMode;

    public DailyRewardCommand(DailyRewardManager dailyManager, boolean fixMode) {
        this.dailyManager = dailyManager;
        this.fixMode      = fixMode;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (fixMode) {
            if (!sender.hasPermission("bob.daily.admin")) {
                sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                return;
            }
            dailyManager.resetAll();
            sender.sendMessage(TextUtil.parse(dailyManager.plugin().getConfig()
                    .getString("daily-reward.messages.reset-all", "&aReset all daily rewards.")));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        if (!dailyManager.plugin().getConfig().getBoolean("daily-reward.enabled", true)) {
            player.sendMessage(Component.text("Daily rewards are currently disabled.", NamedTextColor.RED));
            return;
        }

        if (dailyManager.canClaim(player.getUniqueId())) {
            dailyManager.recordClaim(player.getUniqueId());

            String playerName = player.getName();
            if (!playerName.matches("[a-zA-Z0-9_]{1,16}")) {
                player.sendMessage(Component.text("Your username contains invalid characters.", NamedTextColor.RED));
                return;
            }

            List<String> rewards = dailyManager.plugin().getConfig().getStringList("daily-reward.rewards");
            for (String cmd : rewards) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
            }

            player.sendMessage(TextUtil.parse(dailyManager.plugin().getConfig()
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

            player.sendMessage(TextUtil.parse(msg));
        }
    }
}

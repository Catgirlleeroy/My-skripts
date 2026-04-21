package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
import dev.leeroy.plugin.Utils.punishment.PunishmentHistoryManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class IPTempBanCommand implements BasicCommand {

    private final IPBanManager ipBanManager;
    private final JavaPlugin plugin;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final PunishmentHistoryManager historyManager;

    public IPTempBanCommand(IPBanManager ipBanManager, JavaPlugin plugin,
                             PunishmentDiscordBroadcaster discordBroadcaster,
                             PunishmentHistoryManager historyManager) {
        this.ipBanManager       = ipBanManager;
        this.plugin             = plugin;
        this.discordBroadcaster = discordBroadcaster;
        this.historyManager     = historyManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.iptempban")) {
            sender.sendMessage(Component.text("You don't have permission to IP tempban players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /iptempban <player> <duration> [reason]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Duration examples: 30m, 2h, 1d, 1d12h", NamedTextColor.GRAY));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' must be online to IP tempban.", NamedTextColor.RED));
            return;
        }

        String ip = target.getAddress().getAddress().getHostAddress();
        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily IP banned.";

        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.iptempban")) {
            sender.sendMessage(Component.text(target.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(Component.text("Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.", NamedTextColor.RED));
            return;
        }

        if (ipBanManager.isBanned(ip)) {
            sender.sendMessage(Component.text(target.getName() + "'s IP is already banned.", NamedTextColor.RED));
            return;
        }

        ipBanManager.tempBan(ip, reason, sender.getName(), durationMs);
        historyManager.log(target.getUniqueId(), target.getName(), "tempipban", reason, sender.getName(), durationStr);

        final String finalReason    = reason;
        final long   finalDurationMs = durationMs;
        final String finalIp        = ip;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                online.getWorld().strikeLightningEffect(online.getLocation());
            }
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                    online.kick(Component.text("You have been temporarily IP banned.\n", NamedTextColor.RED)
                            .append(Component.text("Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + finalDurationMs) + "\n", NamedTextColor.WHITE))
                            .append(Component.text("Reason: " + finalReason, NamedTextColor.WHITE)));
                }
            }
        }, 10L);

        TextUtil.broadcast("&c[IP-TEMPBAN] &e" + target.getName() + " &chas been temporarily IP banned! &7Duration: " + durationStr + " | Reason: " + reason);
        discordBroadcaster.broadcast("iptempban", target.getName(), sender.getName(), reason, durationStr);
    }
}

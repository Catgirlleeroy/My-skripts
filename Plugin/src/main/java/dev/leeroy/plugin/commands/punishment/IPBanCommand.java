package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
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

public class IPBanCommand implements BasicCommand {

    private final IPBanManager ipBanManager;
    private final JavaPlugin plugin;
    private final PunishmentDiscordBroadcaster discordBroadcaster;

    public IPBanCommand(IPBanManager ipBanManager, JavaPlugin plugin,
                        PunishmentDiscordBroadcaster discordBroadcaster) {
        this.ipBanManager       = ipBanManager;
        this.plugin             = plugin;
        this.discordBroadcaster = discordBroadcaster;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.ipban")) {
            sender.sendMessage(Component.text("You don't have permission to IP ban players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /ipban <player> [reason]", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' must be online to IP ban.", NamedTextColor.RED));
            return;
        }

        String ip = target.getAddress().getAddress().getHostAddress();
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "You have been IP banned.";

        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.ipban")) {
            sender.sendMessage(Component.text(target.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        if (ipBanManager.isBanned(ip)) {
            sender.sendMessage(Component.text(target.getName() + "'s IP is already banned.", NamedTextColor.RED));
            return;
        }

        ipBanManager.ban(ip, reason, sender.getName());

        final String finalReason = reason;
        final String finalIp = ip;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                online.getWorld().strikeLightningEffect(online.getLocation());
            }
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getAddress().getAddress().getHostAddress().equals(finalIp)) {
                    online.kick(Component.text("You have been permanently IP banned.\n", NamedTextColor.RED)
                            .append(Component.text("Reason: " + finalReason, NamedTextColor.WHITE)));
                }
            }
        }, 10L);

        TextUtil.broadcast("&c[IP-BAN] &e" + target.getName() + " &chas been permanently IP banned! &7Reason: " + reason);
        discordBroadcaster.broadcast("ipban", target.getName(), sender.getName(), reason, null);
    }
}

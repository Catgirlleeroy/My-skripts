package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class KickCommand implements BasicCommand {

    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;

    public KickCommand(VanishManager vanishManager, PunishmentDiscordBroadcaster discordBroadcaster) {
        this.vanishManager      = vanishManager;
        this.discordBroadcaster = discordBroadcaster;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.kick")) {
            sender.sendMessage(Component.text("You don't have permission to kick players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /kick <player> [reason]", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.kick")) {
            sender.sendMessage(Component.text(target.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been kicked.";

        target.kick(Component.text("You have been kicked.\n", NamedTextColor.RED)
                .append(Component.text("Reason: " + reason, NamedTextColor.WHITE)));

        TextUtil.broadcast("&c[KICK] &e" + target.getName() + " &chas been kicked! &7Reason: " + reason);
        discordBroadcaster.broadcast("kick", target.getName(), sender.getName(), reason, null);
    }
}

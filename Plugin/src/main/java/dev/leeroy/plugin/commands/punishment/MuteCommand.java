package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.MuteManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
import dev.leeroy.plugin.Utils.punishment.PunishmentHistoryManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class MuteCommand implements BasicCommand {

    private final MuteManager muteManager;
    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final PunishmentHistoryManager historyManager;

    public MuteCommand(MuteManager muteManager, VanishManager vanishManager,
                       PunishmentDiscordBroadcaster discordBroadcaster,
                       PunishmentHistoryManager historyManager) {
        this.muteManager        = muteManager;
        this.vanishManager      = vanishManager;
        this.discordBroadcaster = discordBroadcaster;
        this.historyManager     = historyManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.mute")) {
            sender.sendMessage(Component.text("You don't have permission to mute players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /mute <player> [reason]", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        String reason = args.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : "You have been muted.";

        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.mute")) {
            sender.sendMessage(Component.text(target.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        if (muteManager.isMuted(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is already muted.", NamedTextColor.RED));
            return;
        }

        muteManager.mute(target.getUniqueId(), target.getName(), reason, sender.getName());
        historyManager.log(target.getUniqueId(), target.getName(), "mute", reason, sender.getName(), null);
        target.sendMessage(Component.text("You have been permanently muted. Reason: " + reason, NamedTextColor.RED));
        TextUtil.broadcast("&c[MUTE] &e" + target.getName() + " &chas been muted! &7Reason: " + reason);
        discordBroadcaster.broadcast("mute", target.getName(), sender.getName(), reason, null);
    }
}

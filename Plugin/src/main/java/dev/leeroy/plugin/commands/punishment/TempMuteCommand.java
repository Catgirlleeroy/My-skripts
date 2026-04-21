package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.BanManager;
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

import java.util.Arrays;
import java.util.Collection;

public class TempMuteCommand implements BasicCommand {

    private final MuteManager muteManager;
    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final PunishmentHistoryManager historyManager;

    public TempMuteCommand(MuteManager muteManager, VanishManager vanishManager,
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

        if (!sender.hasPermission("bob.tempmute")) {
            sender.sendMessage(Component.text("You don't have permission to tempmute players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /tempmute <player> <duration> [reason]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Duration examples: 30m, 2h, 1d", NamedTextColor.GRAY));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily muted.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(Component.text("Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.", NamedTextColor.RED));
            return;
        }

        if (target.hasPermission("bob.exempt") || target.hasPermission("bob.exempt.tempmute")) {
            sender.sendMessage(Component.text(target.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        if (muteManager.isMuted(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is already muted.", NamedTextColor.RED));
            return;
        }

        muteManager.tempMute(target.getUniqueId(), target.getName(), reason, sender.getName(), durationMs);
        historyManager.log(target.getUniqueId(), target.getName(), "tempmute", reason, sender.getName(), durationStr);
        target.sendMessage(Component.text("You have been muted for " + durationStr + ". Reason: " + reason, NamedTextColor.RED));
        TextUtil.broadcast("&c[TEMPMUTE] &e" + target.getName() + " &chas been muted! &7Duration: " + durationStr + " | Reason: " + reason);
        discordBroadcaster.broadcast("tempmute", target.getName(), sender.getName(), reason, durationStr);
    }
}

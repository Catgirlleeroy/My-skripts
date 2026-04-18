package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.ReportManager;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;

public class ReportCommand implements BasicCommand {

    private final ReportManager reportManager;
    private final VanishManager vanishManager;

    public ReportCommand(ReportManager reportManager, VanishManager vanishManager) {
        this.reportManager = reportManager;
        this.vanishManager = vanishManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) return TabUtil.onlinePlayers(stack, args[0], vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player reporter)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        if (!reporter.hasPermission("bob.report")) {
            reporter.sendMessage(Component.text("You don't have permission to report players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            reporter.sendMessage(Component.text("Usage: /report <player> <reason>", NamedTextColor.YELLOW));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            reporter.sendMessage(Component.text("Player '" + targetName + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage(Component.text("You cannot report yourself.", NamedTextColor.RED));
            return;
        }

        String id = reportManager.addReport(reporter.getName(), target.getName(), reason);

        reporter.sendMessage(Component.text("✦ Your report against ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" has been submitted.", NamedTextColor.GREEN)));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("bob.reports")) continue;

            staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
            staff.sendMessage(Component.text("[REPORT] ", NamedTextColor.YELLOW)
                    .append(Component.text(reporter.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" reported ", NamedTextColor.GRAY))
                    .append(Component.text(target.getName(), NamedTextColor.RED)));
            staff.sendMessage(Component.text("Reason: ", NamedTextColor.GRAY)
                    .append(Component.text(reason, NamedTextColor.WHITE)));
            staff.sendMessage(Component.text("Report ID: ", NamedTextColor.GRAY)
                    .append(Component.text(id, NamedTextColor.DARK_GRAY)));

            Component buttons = Component.text("[Punish]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/punish " + target.getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("Open punishment GUI for " + target.getName(), NamedTextColor.GRAY)))
                    .append(Component.text(" "))
                    .append(Component.text("[Teleport]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/tp " + target.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("Teleport to " + target.getName(), NamedTextColor.GRAY))))
                    .append(Component.text(" "))
                    .append(Component.text("[Vanish]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/vanish"))
                            .hoverEvent(HoverEvent.showText(Component.text("Toggle your vanish", NamedTextColor.GRAY))))
                    .append(Component.text(" "))
                    .append(Component.text("[Dismiss]", NamedTextColor.DARK_GRAY)
                            .clickEvent(ClickEvent.runCommand("/reports delete " + id))
                            .hoverEvent(HoverEvent.showText(Component.text("Dismiss this report", NamedTextColor.GRAY))));

            staff.sendMessage(buttons);
            staff.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        }
    }
}

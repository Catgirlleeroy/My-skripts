package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.ReportManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class ReportsCommand implements BasicCommand {

    private final ReportManager reportManager;

    public ReportsCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.reports")) {
            sender.sendMessage(Component.text("You don't have permission to view reports.", NamedTextColor.RED));
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            String id = args[1];
            if (reportManager.deleteReport(id)) {
                sender.sendMessage(Component.text("✦ Report ", NamedTextColor.GREEN)
                        .append(Component.text(id, NamedTextColor.DARK_GRAY))
                        .append(Component.text(" has been dismissed.", NamedTextColor.GREEN)));
            } else {
                sender.sendMessage(Component.text("Report '" + id + "' not found.", NamedTextColor.RED));
            }
            return;
        }

        List<Map<String, String>> reports = reportManager.getReports();

        if (reports.isEmpty()) {
            sender.sendMessage(Component.text("✦ No pending reports.", NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text("Pending Reports (" + reports.size() + ")", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));

        for (Map<String, String> report : reports) {
            String id       = report.get("id");
            String reporter = report.get("reporter");
            String target   = report.get("target");
            String reason   = report.get("reason");
            String time     = report.get("time");

            sender.sendMessage(Component.text("[" + time + "] ", NamedTextColor.GRAY)
                    .append(Component.text(reporter, NamedTextColor.WHITE))
                    .append(Component.text(" → ", NamedTextColor.GRAY))
                    .append(Component.text(target, NamedTextColor.RED))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(reason, NamedTextColor.WHITE)));

            if (sender instanceof Player) {
                Component buttons = Component.text("  ")
                        .append(Component.text("[Punish]", NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/punish " + target))
                                .hoverEvent(HoverEvent.showText(Component.text("Open punishment GUI for " + target, NamedTextColor.GRAY))))
                        .append(Component.text(" "))
                        .append(Component.text("[Teleport]", NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/tp " + target))
                                .hoverEvent(HoverEvent.showText(Component.text("Teleport to " + target, NamedTextColor.GRAY))))
                        .append(Component.text(" "))
                        .append(Component.text("[Vanish]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/vanish"))
                                .hoverEvent(HoverEvent.showText(Component.text("Toggle your vanish", NamedTextColor.GRAY))))
                        .append(Component.text(" "))
                        .append(Component.text("[Dismiss]", NamedTextColor.DARK_GRAY)
                                .clickEvent(ClickEvent.runCommand("/reports delete " + id))
                                .hoverEvent(HoverEvent.showText(Component.text("Dismiss this report", NamedTextColor.GRAY))));

                sender.sendMessage(buttons);
            }

            sender.sendMessage(Component.text("ID: " + id, NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.empty());
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
    }
}

package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.ReportManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class ReportsCommand implements CommandExecutor {

    private final ReportManager reportManager;

    public ReportsCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bob.reports")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view reports.");
            return true;
        }

        // /reports delete <id>
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            String id = args[1];
            if (reportManager.deleteReport(id)) {
                sender.sendMessage(ChatColor.GREEN + "✦ Report " + ChatColor.DARK_GRAY + id + ChatColor.GREEN + " has been dismissed.");
            } else {
                sender.sendMessage(ChatColor.RED + "Report '" + id + "' not found.");
            }
            return true;
        }

        // /reports — list all
        List<Map<String, String>> reports = reportManager.getReports();

        if (reports.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "✦ No pending reports.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "Pending Reports (" + reports.size() + ")");
        sender.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (Map<String, String> report : reports) {
            String id       = report.get("id");
            String reporter = report.get("reporter");
            String target   = report.get("target");
            String reason   = report.get("reason");
            String time     = report.get("time");

            sender.sendMessage(ChatColor.GRAY + "[" + time + "] "
                    + ChatColor.WHITE + reporter + ChatColor.GRAY + " → "
                    + ChatColor.RED + target
                    + ChatColor.GRAY + ": " + ChatColor.WHITE + reason);

            if (sender instanceof Player staff) {
                // Clickable buttons
                TextComponent punishBtn = new TextComponent(ChatColor.RED + "[Punish]");
                punishBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/punish " + target));
                punishBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(ChatColor.GRAY + "Open punishment GUI for " + target).create()));

                TextComponent space1 = new TextComponent(" ");

                TextComponent tpBtn = new TextComponent(ChatColor.AQUA + "[Teleport]");
                tpBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + target));
                tpBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(ChatColor.GRAY + "Teleport to " + target).create()));

                TextComponent space2 = new TextComponent(" ");

                TextComponent vanishBtn = new TextComponent(ChatColor.GREEN + "[Vanish]");
                vanishBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vanish"));
                vanishBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(ChatColor.GRAY + "Toggle your vanish").create()));

                TextComponent space3 = new TextComponent(" ");

                TextComponent deleteBtn = new TextComponent(ChatColor.DARK_GRAY + "[Dismiss]");
                deleteBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports delete " + id));
                deleteBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(ChatColor.GRAY + "Dismiss this report").create()));

                TextComponent line = new TextComponent("  ");
                line.addExtra(punishBtn);
                line.addExtra(space1);
                line.addExtra(tpBtn);
                line.addExtra(space2);
                line.addExtra(vanishBtn);
                line.addExtra(space3);
                line.addExtra(deleteBtn);

                staff.spigot().sendMessage(line);
            }

            sender.sendMessage(ChatColor.DARK_GRAY + "ID: " + id);
            sender.sendMessage("");
        }

        sender.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }
}
package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.ReportManager;
import dev.leeroy.plugin.Utils.VanishManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class ReportCommand implements CommandExecutor {

    private final ReportManager reportManager;
    private final VanishManager vanishManager;

    public ReportCommand(ReportManager reportManager, VanishManager vanishManager) {
        this.reportManager = reportManager;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!reporter.hasPermission("bob.report")) {
            reporter.sendMessage(ChatColor.RED + "You don't have permission to report players.");
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage(ChatColor.YELLOW + "Usage: /report <player> <reason>");
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            reporter.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or is offline.");
            return true;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage(ChatColor.RED + "You cannot report yourself.");
            return true;
        }

        String id = reportManager.addReport(reporter.getName(), target.getName(), reason);

        reporter.sendMessage(ChatColor.GREEN + "✦ Your report against " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " has been submitted.");

        // Notify all online staff with bob.reports permission
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("bob.reports")) continue;

            // Header
            staff.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            staff.sendMessage(ChatColor.YELLOW + "[REPORT] " + ChatColor.WHITE + reporter.getName()
                    + ChatColor.GRAY + " reported " + ChatColor.RED + target.getName());
            staff.sendMessage(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason);
            staff.sendMessage(ChatColor.GRAY + "Report ID: " + ChatColor.DARK_GRAY + id);

            // Clickable action buttons
            TextComponent punishBtn = new TextComponent(ChatColor.RED + "[Punish]");
            punishBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/punish " + target.getName()));
            punishBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(ChatColor.GRAY + "Open punishment GUI for " + target.getName()).create()));

            TextComponent space1 = new TextComponent(" ");

            TextComponent tpBtn = new TextComponent(ChatColor.AQUA + "[Teleport]");
            tpBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + target.getName()));
            tpBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(ChatColor.GRAY + "Teleport to " + target.getName()).create()));

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

            TextComponent line = new TextComponent("");
            line.addExtra(punishBtn);
            line.addExtra(space1);
            line.addExtra(tpBtn);
            line.addExtra(space2);
            line.addExtra(vanishBtn);
            line.addExtra(space3);
            line.addExtra(deleteBtn);

            staff.spigot().sendMessage(line);
            staff.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        return true;
    }
}
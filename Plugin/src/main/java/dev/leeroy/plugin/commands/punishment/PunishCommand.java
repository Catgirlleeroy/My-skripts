package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.punishment.PunishConfig;
import dev.leeroy.plugin.gui.PunishGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PunishCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PunishConfig punishConfig;

    public PunishCommand(JavaPlugin plugin, PunishConfig punishConfig) {
        this.plugin = plugin;
        this.punishConfig = punishConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!staff.hasPermission("bob.punish")) {
            staff.sendMessage(ChatColor.RED + "You don't have permission to punish players.");
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage(ChatColor.YELLOW + "Usage: /punish <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        if (target.equals(staff)) {
            staff.sendMessage(ChatColor.RED + "You cannot punish yourself.");
            return true;
        }

        PunishGUI.openActionGUI(staff, target, punishConfig.get());
        return true;
    }
}

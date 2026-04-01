package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CombatCommand implements CommandExecutor {

    private final CombatManager combatManager;
    private final boolean setMode; // true = setcombat, false = removecombat

    public CombatCommand(CombatManager combatManager, boolean setMode) {
        this.combatManager = combatManager;
        this.setMode       = setMode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bob.combat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player.");
                return true;
            }
            target = (Player) sender;
        }

        if (setMode) {
            combatManager.tag(target);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " is now combat tagged.");
        } else {
            combatManager.untag(target);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " is no longer combat tagged.");
        }

        return true;
    }
}
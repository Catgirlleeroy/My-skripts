package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.PlayerHealer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // No arguments — heal self
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /heal <player>");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("healplugin.heal")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            PlayerHealer.heal(player);
            player.sendMessage(ChatColor.GREEN + "You have been healed!");
            return true;
        }

        // One argument — heal another player
        if (args.length == 1) {
            if (!sender.hasPermission("healplugin.heal.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to heal other players.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }

            PlayerHealer.heal(target);
            target.sendMessage(ChatColor.GREEN + "You have been healed by " + sender.getName() + "!");
            sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName() + ".");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /heal [player]");
        return true;
    }
}
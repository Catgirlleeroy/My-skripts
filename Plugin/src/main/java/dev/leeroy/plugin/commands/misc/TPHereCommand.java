package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPHereCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public TPHereCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /tphere.");
            return true;
        }

        if (!sender.hasPermission("bob.tp.here")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use /tphere.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tphere <player>");
            return true;
        }

        Player self = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        if (target.equals(self)) {
            sender.sendMessage(ChatColor.RED + "You cannot teleport yourself to yourself.");
            return true;
        }

        // Pulling a player to a vanished sender would reveal the sender's hidden location
        if (vanishManager.isVanished(self.getUniqueId()) && !target.hasPermission("bob.vanish.see")) {
            sender.sendMessage(ChatColor.RED + "You cannot pull players while vanished.");
            return true;
        }

        target.teleport(self.getLocation());
        target.sendMessage(ChatColor.GREEN + "You have been teleported to " + self.getName() + ".");
        sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to you.");
        return true;
    }
}

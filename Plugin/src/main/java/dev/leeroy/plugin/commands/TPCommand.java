package dev.leeroy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /tp <player> — teleport self to player
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a target: /tp <player1> <player2>");
                return true;
            }

            if (!sender.hasPermission("bob.tp.self")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to teleport.");
                return true;
            }

            Player self = (Player) sender;
            Player target = Bukkit.getPlayerExact(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }

            self.teleport(target.getLocation());
            self.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
            return true;
        }

        // /tp <x> <y> <z> — teleport self to coordinates
        if (args.length == 3) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot teleport to coordinates.");
                return true;
            }

            if (!sender.hasPermission("bob.tp.coords")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to teleport to coordinates.");
                return true;
            }

            Player self = (Player) sender;

            double x, y, z;
            try {
                x = parseCoord(args[0], self.getLocation().getX());
                y = parseCoord(args[1], self.getLocation().getY());
                z = parseCoord(args[2], self.getLocation().getZ());
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates. Use numbers or ~ for relative.");
                return true;
            }

            Location dest = new Location(self.getWorld(), x, y, z,
                    self.getLocation().getYaw(), self.getLocation().getPitch());
            self.teleport(dest);
            self.sendMessage(ChatColor.GREEN + "Teleported to " + format(x) + ", " + format(y) + ", " + format(z) + ".");
            return true;
        }

        // /tp <player1> <player2> — teleport player1 to player2
        if (args.length == 2) {
            if (!sender.hasPermission("bob.tp.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to teleport other players.");
                return true;
            }

            Player player1 = Bukkit.getPlayerExact(args[0]);
            Player player2 = Bukkit.getPlayerExact(args[1]);

            if (player1 == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }
            if (player2 == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found or is offline.");
                return true;
            }

            player1.teleport(player2.getLocation());
            player1.sendMessage(ChatColor.GREEN + "You have been teleported to " + player2.getName()
                    + " by " + sender.getName() + ".");
            sender.sendMessage(ChatColor.GREEN + "Teleported " + player1.getName()
                    + " to " + player2.getName() + ".");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "  /tp <player>");
        sender.sendMessage(ChatColor.YELLOW + "  /tp <x> <y> <z>");
        sender.sendMessage(ChatColor.YELLOW + "  /tp <player1> <player2>");
        return true;
    }

    /**
     * Parses a coordinate value. Supports ~ for relative coords.
     */
    private double parseCoord(String input, double current) {
        if (input.startsWith("~")) {
            double offset = input.length() > 1 ? Double.parseDouble(input.substring(1)) : 0;
            return current + offset;
        }
        return Double.parseDouble(input);
    }

    private String format(double value) {
        return String.format("%.1f", value);
    }
}
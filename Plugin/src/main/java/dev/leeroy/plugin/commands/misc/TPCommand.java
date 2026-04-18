package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class TPCommand implements BasicCommand {

    private final VanishManager vanishManager;

    public TPCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 2) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, args.length > 0 ? args.length - 1 : 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        // /tp <player> — teleport self to player
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(Component.text("Console must specify a target: /tp <player1> <player2>", NamedTextColor.RED));
                return;
            }

            if (!sender.hasPermission("bob.tp.self")) {
                sender.sendMessage(Component.text("You don't have permission to teleport.", NamedTextColor.RED));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);

            if (target == null) {
                sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            if (target.equals(self)) {
                sender.sendMessage(Component.text("You cannot teleport to yourself.", NamedTextColor.RED));
                return;
            }

            if (vanishManager.isVanished(target.getUniqueId()) && !sender.hasPermission("bob.vanish.see")) {
                sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            self.teleport(target.getLocation());
            self.sendMessage(Component.text("Teleported to " + target.getName() + ".", NamedTextColor.GREEN));
            return;
        }

        // /tp <x> <y> <z> — teleport self to coordinates
        if (args.length == 3) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(Component.text("Console cannot teleport to coordinates.", NamedTextColor.RED));
                return;
            }

            if (!sender.hasPermission("bob.tp.coords")) {
                sender.sendMessage(Component.text("You don't have permission to teleport to coordinates.", NamedTextColor.RED));
                return;
            }

            double x, y, z;
            try {
                x = parseCoord(args[0], self.getLocation().getX());
                y = parseCoord(args[1], self.getLocation().getY());
                z = parseCoord(args[2], self.getLocation().getZ());
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid coordinates. Use numbers or ~ for relative.", NamedTextColor.RED));
                return;
            }

            Location dest = new Location(self.getWorld(), x, y, z,
                    self.getLocation().getYaw(), self.getLocation().getPitch());
            self.teleport(dest);
            self.sendMessage(Component.text("Teleported to " + format(x) + ", " + format(y) + ", " + format(z) + ".", NamedTextColor.GREEN));
            return;
        }

        // /tp <player1> <player2> — teleport player1 to player2
        if (args.length == 2) {
            if (!sender.hasPermission("bob.tp.others")) {
                sender.sendMessage(Component.text("You don't have permission to teleport other players.", NamedTextColor.RED));
                return;
            }

            Player player1 = Bukkit.getPlayerExact(args[0]);
            Player player2 = Bukkit.getPlayerExact(args[1]);

            if (player1 == null) {
                sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }
            if (player2 == null) {
                sender.sendMessage(Component.text("Player '" + args[1] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            if (player1.equals(player2)) {
                sender.sendMessage(Component.text("Cannot teleport a player to themselves.", NamedTextColor.RED));
                return;
            }

            if (vanishManager.isVanished(player2.getUniqueId()) && !sender.hasPermission("bob.vanish.see")) {
                sender.sendMessage(Component.text("Player '" + args[1] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            player1.teleport(player2.getLocation());
            player1.sendMessage(Component.text("You have been teleported to " + player2.getName()
                    + " by " + sender.getName() + ".", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Teleported " + player1.getName()
                    + " to " + player2.getName() + ".", NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /tp <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /tp <x> <y> <z>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /tp <player1> <player2>", NamedTextColor.YELLOW));
    }

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

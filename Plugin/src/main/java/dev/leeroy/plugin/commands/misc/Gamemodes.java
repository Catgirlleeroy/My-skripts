package dev.leeroy.plugin.commands.misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Gamemodes implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Resolve the target gamemode from the command label (gm0/gm1/gm2/gm3)
        GameMode targetMode = resolveGameMode(label);
        if (targetMode == null) {
            sender.sendMessage(ChatColor.RED + "Unknown gamemode command.");
            return true;
        }

        // No arguments — change own gamemode
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /" + label + " <player>");
                return true;
            }

            Player player = (Player) sender;
            String selfPerm = "bob.gamemode.self." + targetMode.name().toLowerCase();

            if (!player.hasPermission(selfPerm) && !player.hasPermission("bob.gamemode.self")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to change your gamemode to " + formatGameMode(targetMode) + ".");
                return true;
            }

            player.setGameMode(targetMode);
            player.sendMessage(ChatColor.GREEN + "Your gamemode has been set to " + formatGameMode(targetMode) + ".");
            return true;
        }

        // One argument — change another player's gamemode
        if (args.length == 1) {
            String othersPerm = "bob.gamemode.others." + targetMode.name().toLowerCase();

            if (!sender.hasPermission(othersPerm) && !sender.hasPermission("bob.gamemode.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to change other players' gamemode to " + formatGameMode(targetMode) + ".");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }

            target.setGameMode(targetMode);
            target.sendMessage(ChatColor.GREEN + "Your gamemode has been set to "
                    + formatGameMode(targetMode) + " by " + sender.getName() + ".");
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName()
                    + "'s gamemode to " + formatGameMode(targetMode) + ".");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " [player]");
        return true;
    }

    /**
     * Maps a command label to its corresponding GameMode.
     * Supports both short aliases (gm0–gm3) and the base "gamemode" command with a numeric arg.
     */
    private GameMode resolveGameMode(String label) {
        return switch (label.toLowerCase()) {
            case "gm0" -> GameMode.SURVIVAL;
            case "gm1" -> GameMode.CREATIVE;
            case "gm2" -> GameMode.ADVENTURE;
            case "gm3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    /** Returns a nicely formatted gamemode name. */
    private String formatGameMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL  -> "Survival";
            case CREATIVE  -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }
}
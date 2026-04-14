package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.listeners.misc.CommandSpyListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpyCommand implements CommandExecutor {

    private final CommandSpyListener commandSpyListener;

    public CommandSpyCommand(CommandSpyListener commandSpyListener) {
        this.commandSpyListener = commandSpyListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("bob.commandspy")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use CommandSpy.");
            return true;
        }

        Player player = (Player) sender;
        boolean isNowEnabled = commandSpyListener.toggle(player);

        if (isNowEnabled) {
            player.sendMessage(ChatColor.GREEN + "CommandSpy enabled. You will now see commands run by others.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "CommandSpy disabled.");
        }

        return true;
    }
}
package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.VanishManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("bob.vanish")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to vanish.");
            return true;
        }

        boolean isNowVanished = vanishManager.toggle(player);

        if (isNowVanished) {
            player.sendMessage(ChatColor.GRAY + "✦ You are now " + ChatColor.GREEN + "vanished" + ChatColor.GRAY + ". Staff with " + ChatColor.YELLOW + "bob.vanish.see" + ChatColor.GRAY + " can still see you.");
        } else {
            player.sendMessage(ChatColor.GRAY + "✦ You are now " + ChatColor.RED + "visible" + ChatColor.GRAY + ".");
        }

        return true;
    }
}
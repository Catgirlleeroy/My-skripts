package dev.leeroy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatClearCommand implements CommandExecutor {

    // Blank lines to push old chat off screen
    private static final String BLANK_LINES = "\n".repeat(100);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.chatclear")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to clear chat.");
            return true;
        }

        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(BLANK_LINES));
        Bukkit.broadcastMessage(ChatColor.YELLOW + "[CHAT] Chat was cleared by " + sender.getName() + ".");
        return true;
    }
}
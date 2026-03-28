package dev.leeroy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatClearCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.chatclear")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to clear chat.");
            return true;
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            for (int i = 0; i < 100; i++) {
                p.sendMessage(" ");
            }
        });
        Bukkit.broadcastMessage(ChatColor.YELLOW + "[CHAT] Chat was cleared by " + sender.getName() + ".");
        return true;
    }
}
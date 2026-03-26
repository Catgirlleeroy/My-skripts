package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.listeners.MuteListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatMuteCommand implements CommandExecutor {

    private final MuteListener muteListener;

    public ChatMuteCommand(MuteListener muteListener) {
        this.muteListener = muteListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.chatmute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to toggle chat mute.");
            return true;
        }

        boolean isNowMuted = muteListener.toggleChatMute();

        if (isNowMuted) {
            Bukkit.broadcastMessage(ChatColor.RED + "[CHAT] " + ChatColor.WHITE + "Chat has been muted by " + sender.getName() + ".");
        } else {
            Bukkit.broadcastMessage(ChatColor.GREEN + "[CHAT] " + ChatColor.WHITE + "Chat has been unmuted by " + sender.getName() + ".");
        }

        return true;
    }
}
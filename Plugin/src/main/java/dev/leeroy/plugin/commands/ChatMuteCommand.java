package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.listeners.punishment.MuteListener;
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
            // High pitch anvil slam for mute
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f)
            );
        } else {
            Bukkit.broadcastMessage(ChatColor.GREEN + "[CHAT] " + ChatColor.WHITE + "Chat has been unmuted by " + sender.getName() + ".");
            // Evoker prepare summon sound for unmute
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f)
            );
        }

        return true;
    }
}
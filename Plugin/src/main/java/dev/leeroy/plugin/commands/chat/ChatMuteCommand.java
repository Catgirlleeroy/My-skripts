package dev.leeroy.plugin.commands.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.listeners.punishment.MuteListener;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

public class ChatMuteCommand implements BasicCommand {

    private final MuteListener muteListener;

    public ChatMuteCommand(MuteListener muteListener) {
        this.muteListener = muteListener;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.chatmute")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to toggle chat mute.", NamedTextColor.RED)));
            return;
        }

        boolean isNowMuted = muteListener.toggleChatMute();

        if (isNowMuted) {
            TextUtil.broadcast("&c[CHAT] &fChat has been muted by " + sender.getName() + ".");
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f)
            );
        } else {
            TextUtil.broadcast("&a[CHAT] &fChat has been unmuted by " + sender.getName() + ".");
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f)
            );
        }
    }
}

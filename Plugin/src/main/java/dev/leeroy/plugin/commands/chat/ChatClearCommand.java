package dev.leeroy.plugin.commands.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ChatClearCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.chatclear")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to clear chat.", NamedTextColor.RED)));
            return;
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            for (int i = 0; i < 100; i++) {
                p.sendMessage(Component.empty());
            }
        });
        TextUtil.broadcast("&e[CHAT] &fChat was cleared by " + sender.getName() + ".");
    }
}

package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.listeners.misc.CommandSpyListener;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpyCommand implements BasicCommand {

    private final CommandSpyListener commandSpyListener;

    public CommandSpyCommand(CommandSpyListener commandSpyListener) {
        this.commandSpyListener = commandSpyListener;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Only players can use this command.", NamedTextColor.RED)));
            return;
        }

        if (!sender.hasPermission("bob.commandspy")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to use CommandSpy.", NamedTextColor.RED)));
            return;
        }

        boolean isNowEnabled = commandSpyListener.toggle(player);

        if (isNowEnabled) {
            player.sendMessage(TextUtil.prefixed(Component.text("CommandSpy enabled. You will now see commands run by others.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(TextUtil.prefixed(Component.text("CommandSpy disabled.", NamedTextColor.YELLOW)));
        }
    }
}

package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.PlayerHealer;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class HealCommand implements BasicCommand {

    private final VanishManager vanishManager;

    public HealCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a player: /heal <player>", NamedTextColor.RED));
                return;
            }

            if (!player.hasPermission("bob.heal")) {
                player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return;
            }

            PlayerHealer.heal(player);
            player.sendMessage(Component.text("You have been healed!", NamedTextColor.GREEN));
            return;
        }

        if (args.length == 1) {
            if (!sender.hasPermission("bob.heal.others")) {
                sender.sendMessage(Component.text("You don't have permission to heal other players.", NamedTextColor.RED));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            PlayerHealer.heal(target);
            target.sendMessage(Component.text("You have been healed by " + sender.getName() + "!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Healed " + target.getName() + ".", NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("Usage: /heal [player]", NamedTextColor.YELLOW));
    }
}

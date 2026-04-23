package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class TPHereCommand implements BasicCommand {

    private final VanishManager vanishManager;

    public TPHereCommand(VanishManager vanishManager) {
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

        if (!(sender instanceof Player self)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Only players can use /tphere.", NamedTextColor.RED)));
            return;
        }

        if (!sender.hasPermission("bob.tp.here")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to use /tphere.", NamedTextColor.RED)));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Usage: /tphere <player>", NamedTextColor.YELLOW)));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED)));
            return;
        }

        if (target.equals(self)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You cannot teleport yourself to yourself.", NamedTextColor.RED)));
            return;
        }

        if (vanishManager.isVanished(self.getUniqueId()) && !target.hasPermission("bob.vanish.see")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You cannot pull players while vanished.", NamedTextColor.RED)));
            return;
        }

        target.teleport(self.getLocation());
        target.sendMessage(TextUtil.prefixed(Component.text("You have been teleported to " + self.getName() + ".", NamedTextColor.GREEN)));
        sender.sendMessage(TextUtil.prefixed(Component.text("Teleported " + target.getName() + " to you.", NamedTextColor.GREEN)));
    }
}

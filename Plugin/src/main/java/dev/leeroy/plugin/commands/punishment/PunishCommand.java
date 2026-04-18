package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.PunishConfig;
import dev.leeroy.plugin.gui.PunishGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public class PunishCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final PunishConfig punishConfig;
    private final VanishManager vanishManager;

    public PunishCommand(JavaPlugin plugin, PunishConfig punishConfig, VanishManager vanishManager) {
        this.plugin        = plugin;
        this.punishConfig  = punishConfig;
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

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return;
        }

        if (!staff.hasPermission("bob.punish")) {
            staff.sendMessage(Component.text("You don't have permission to punish players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            staff.sendMessage(Component.text("Usage: /punish <player>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            staff.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        if (target.equals(staff)) {
            staff.sendMessage(Component.text("You cannot punish yourself.", NamedTextColor.RED));
            return;
        }

        PunishGUI.openActionGUI(staff, target, punishConfig.get());
    }
}

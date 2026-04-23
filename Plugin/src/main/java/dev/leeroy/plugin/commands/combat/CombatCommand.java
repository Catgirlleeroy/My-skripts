package dev.leeroy.plugin.commands.combat;

import dev.leeroy.plugin.Utils.combat.CombatManager;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CombatCommand implements BasicCommand {

    private final CombatManager combatManager;
    private final boolean setMode;

    public CombatCommand(CombatManager combatManager, boolean setMode) {
        this.combatManager = combatManager;
        this.setMode       = setMode;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length > 1) return java.util.Collections.emptyList();
        String lower = (args.length == 1 ? args[0] : "").toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(lower)) result.add(p.getName());
        }
        return result;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.combat.admin")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission.", NamedTextColor.RED)));
            return;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(TextUtil.prefixed(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED)));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextUtil.prefixed(Component.text("Console must specify a player.", NamedTextColor.RED)));
                return;
            }
            target = (Player) sender;
        }

        if (setMode) {
            combatManager.tag(target);
            sender.sendMessage(TextUtil.prefixed(Component.text(target.getName() + " is now combat tagged.", NamedTextColor.GREEN)));
        } else {
            combatManager.untag(target);
            sender.sendMessage(TextUtil.prefixed(Component.text(target.getName() + " is no longer combat tagged.", NamedTextColor.GREEN)));
        }
    }
}

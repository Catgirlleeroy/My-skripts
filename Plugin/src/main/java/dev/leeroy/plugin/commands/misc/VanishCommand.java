package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements BasicCommand {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Only players can use this command.", NamedTextColor.RED)));
            return;
        }

        if (!player.hasPermission("bob.vanish")) {
            player.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to vanish.", NamedTextColor.RED)));
            return;
        }

        boolean isNowVanished = vanishManager.toggle(player);

        if (isNowVanished) {
            player.sendMessage(TextUtil.prefixed(Component.text("✦ You are now ", NamedTextColor.GRAY)
                    .append(Component.text("vanished", NamedTextColor.GREEN))
                    .append(Component.text(". Staff with ", NamedTextColor.GRAY))
                    .append(Component.text("bob.vanish.see", NamedTextColor.YELLOW))
                    .append(Component.text(" can still see you.", NamedTextColor.GRAY))));
        } else {
            player.sendMessage(TextUtil.prefixed(Component.text("✦ You are now ", NamedTextColor.GRAY)
                    .append(Component.text("visible", NamedTextColor.RED))
                    .append(Component.text(".", NamedTextColor.GRAY))));
        }
    }
}

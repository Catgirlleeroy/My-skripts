package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CheckIPBanCommand implements BasicCommand {

    private final IPBanManager ipBanManager;

    public CheckIPBanCommand(IPBanManager ipBanManager) {
        this.ipBanManager = ipBanManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.checkipban")) {
            sender.sendMessage(Component.text("You don't have permission to check IP bans.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /checkipban <player|ip>", NamedTextColor.YELLOW));
            return;
        }

        String input = args[0];
        String ip;
        Player target = Bukkit.getPlayerExact(input);
        ip = target != null ? target.getAddress().getAddress().getHostAddress() : input;

        Map<String, Object> details = ipBanManager.getBanDetails(ip);
        if (details == null) {
            sender.sendMessage(Component.text("IP " + ip + " is not currently banned.", NamedTextColor.GREEN));
            return;
        }

        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        sender.sendMessage(TextUtil.parse("&6━━━ IP Ban Info: " + ip + " ━━━"));
        sender.sendMessage(TextUtil.parse("&eType: &f"      + (type.equals("permanent") ? "Permanent" : "Temporary")));
        sender.sendMessage(TextUtil.parse("&eReason: &f"    + reason));
        sender.sendMessage(TextUtil.parse("&eBanned by: &f" + bannedBy));
        if (expiry != -1L) {
            sender.sendMessage(TextUtil.parse("&eExpires in: &f" + BanManager.formatRemaining(expiry)));
        }
    }
}

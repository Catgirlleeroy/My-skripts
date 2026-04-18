package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CheckBanCommand implements BasicCommand {

    private final BanManager banManager;
    private final PlayerCache playerCache;

    public CheckBanCommand(BanManager banManager, PlayerCache playerCache) {
        this.banManager  = banManager;
        this.playerCache = playerCache;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.checkban")) {
            sender.sendMessage(Component.text("You don't have permission to check bans.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /checkban <player|uuid>", NamedTextColor.YELLOW));
            return;
        }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) {
            sender.sendMessage(Component.text("Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.", NamedTextColor.RED));
            return;
        }

        Map<String, Object> details = banManager.getBanDetails(uuid);
        if (details == null) {
            String name = playerCache.getName(uuid);
            sender.sendMessage(Component.text((name != null ? name : args[0]) + " is not currently banned.", NamedTextColor.GREEN));
            return;
        }

        String name     = (String) details.get("name");
        String type     = (String) details.get("type");
        String reason   = (String) details.get("reason");
        String bannedBy = (String) details.get("bannedBy");
        long   expiry   = (long)   details.get("expiry");

        sender.sendMessage(TextUtil.parse("&6━━━ Ban Info: " + name + " ━━━"));
        sender.sendMessage(TextUtil.parse("&eUUID: &f"      + uuid));
        sender.sendMessage(TextUtil.parse("&eType: &f"      + (type.equals("permanent") ? "Permanent" : "Temporary")));
        sender.sendMessage(TextUtil.parse("&eReason: &f"    + reason));
        sender.sendMessage(TextUtil.parse("&eBanned by: &f" + bannedBy));
        if (expiry != -1L) {
            sender.sendMessage(TextUtil.parse("&eExpires in: &f" + BanManager.formatRemaining(expiry)));
        }
    }

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return playerCache.getUUID(input);
    }
}

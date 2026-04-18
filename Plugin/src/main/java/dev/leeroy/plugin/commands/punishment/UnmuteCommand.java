package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.punishment.MuteManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class UnmuteCommand implements BasicCommand {

    private final MuteManager muteManager;
    private final PlayerCache playerCache;

    public UnmuteCommand(MuteManager muteManager, PlayerCache playerCache) {
        this.muteManager = muteManager;
        this.playerCache = playerCache;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.cachedPlayers(playerCache, TabUtil.arg(args, 0));
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.unmute")) {
            sender.sendMessage(Component.text("You don't have permission to unmute players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /unmute <player|uuid>", NamedTextColor.YELLOW));
            return;
        }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) {
            sender.sendMessage(Component.text("Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.", NamedTextColor.RED));
            return;
        }

        if (!muteManager.isMuted(uuid)) {
            sender.sendMessage(Component.text(args[0] + " is not currently muted.", NamedTextColor.RED));
            return;
        }

        muteManager.unmute(uuid);
        String name = playerCache.getName(uuid);
        sender.sendMessage(Component.text((name != null ? name : uuid.toString()) + " has been unmuted.", NamedTextColor.GREEN));

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) target.sendMessage(Component.text("You have been unmuted.", NamedTextColor.GREEN));
    }

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return playerCache.getUUID(input);
    }
}

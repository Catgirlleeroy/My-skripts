package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IPUnbanCommand implements BasicCommand {

    private final IPBanManager ipBanManager;
    private final PlayerCache playerCache;

    public IPUnbanCommand(IPBanManager ipBanManager, PlayerCache playerCache) {
        this.ipBanManager = ipBanManager;
        this.playerCache  = playerCache;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.ipunban")) {
            sender.sendMessage(Component.text("You don't have permission to unban IPs.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /ipunban <player|uuid|ip>", NamedTextColor.YELLOW));
            return;
        }

        String input = args[0];
        String ip = resolveToIP(input);

        if (ip == null) {
            sender.sendMessage(Component.text("Could not resolve '" + input + "' to an IP. Player must be online, in cache, or provide a raw IP.", NamedTextColor.RED));
            return;
        }

        if (!ipBanManager.isBanned(ip)) {
            sender.sendMessage(Component.text("IP " + ip + " is not currently banned.", NamedTextColor.RED));
            return;
        }

        ipBanManager.unban(ip);
        sender.sendMessage(Component.text("IP " + ip + " (" + input + ") has been unbanned.", NamedTextColor.GREEN));
    }

    private String resolveToIP(String input) {
        Player byName = Bukkit.getPlayerExact(input);
        if (byName != null) return byName.getAddress().getAddress().getHostAddress();

        String cachedIP = playerCache.getIPByName(input);
        if (cachedIP != null) return cachedIP;

        try {
            UUID uuid = UUID.fromString(input);
            Player byUUID = Bukkit.getPlayer(uuid);
            if (byUUID != null) return byUUID.getAddress().getAddress().getHostAddress();
            String ipByUUID = playerCache.getIP(uuid);
            if (ipByUUID != null) return ipByUUID;
        } catch (IllegalArgumentException ignored) {}

        if (input.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) return input;
        return null;
    }
}

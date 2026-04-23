package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.MuteManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
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
    private final PunishmentDiscordBroadcaster discordBroadcaster;

    public UnmuteCommand(MuteManager muteManager, PlayerCache playerCache,
                         PunishmentDiscordBroadcaster discordBroadcaster) {
        this.muteManager        = muteManager;
        this.playerCache        = playerCache;
        this.discordBroadcaster = discordBroadcaster;
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
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to unmute players.", NamedTextColor.RED)));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Usage: /unmute <player|uuid>", NamedTextColor.YELLOW)));
            return;
        }

        UUID uuid = playerCache.resolveUUID(args[0]);
        if (uuid == null) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Could not find '" + args[0] + "'. Use their UUID or make sure their name is in the cache.", NamedTextColor.RED)));
            return;
        }

        if (!muteManager.isMuted(uuid)) {
            sender.sendMessage(TextUtil.prefixed(Component.text(args[0] + " is not currently muted.", NamedTextColor.RED)));
            return;
        }

        muteManager.unmute(uuid);
        String name = playerCache.getName(uuid);
        String displayName = name != null ? name : uuid.toString();
        discordBroadcaster.broadcast("unmute", displayName, sender.getName(), null, null);
        sender.sendMessage(TextUtil.prefixed(Component.text(displayName + " has been unmuted.", NamedTextColor.GREEN)));

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) target.sendMessage(TextUtil.prefixed(Component.text("You have been unmuted.", NamedTextColor.GREEN)));
    }
}

package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.PunishmentDiscordBroadcaster;
import dev.leeroy.plugin.Utils.punishment.PunishmentHistoryManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class BanCommand implements BasicCommand {

    private final BanManager banManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final PunishmentHistoryManager historyManager;

    public BanCommand(BanManager banManager, PlayerCache playerCache, JavaPlugin plugin,
                      VanishManager vanishManager, PunishmentDiscordBroadcaster discordBroadcaster,
                      PunishmentHistoryManager historyManager) {
        this.banManager          = banManager;
        this.playerCache         = playerCache;
        this.plugin              = plugin;
        this.vanishManager       = vanishManager;
        this.discordBroadcaster  = discordBroadcaster;
        this.historyManager      = historyManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.ban")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to ban players.", NamedTextColor.RED)));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Usage: /ban <player|uuid> [reason]", NamedTextColor.YELLOW)));
            return;
        }

        String input  = args[0];
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "You have been banned.";

        UUID   uuid;
        String targetName;
        Player onlineTarget = Bukkit.getPlayerExact(input);

        if (onlineTarget != null) {
            uuid       = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            uuid = playerCache.resolveUUID(input);
            if (uuid == null) {
                sender.sendMessage(TextUtil.prefixed(Component.text("Player '" + input + "' not found. They must have joined before, or provide a UUID.", NamedTextColor.RED)));
                return;
            }
            String cached = playerCache.getName(uuid);
            targetName = cached != null ? cached : uuid.toString();
        }

        if (onlineTarget != null && (onlineTarget.hasPermission("bob.exempt") || onlineTarget.hasPermission("bob.exempt.ban"))) {
            sender.sendMessage(TextUtil.prefixed(Component.text(onlineTarget.getName() + " is exempt from this punishment.", NamedTextColor.RED)));
            return;
        }

        if (banManager.isBanned(uuid)) {
            sender.sendMessage(TextUtil.prefixed(Component.text(targetName + " is already banned.", NamedTextColor.RED)));
            return;
        }

        banManager.ban(uuid, targetName, reason, sender.getName());
        historyManager.log(uuid, targetName, "ban", reason, sender.getName(), null);

        if (onlineTarget != null) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));
            final String finalReason = reason;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    onlineTarget.kick(Component.text("You have been permanently banned.\n", NamedTextColor.RED)
                            .append(Component.text("Reason: " + finalReason, NamedTextColor.WHITE))), 10L);
        }

        TextUtil.broadcast("&c[BAN] &e" + targetName + " &chas been permanently banned! &7Reason: " + reason);
        discordBroadcaster.broadcast("ban", targetName, sender.getName(), reason, null);
        sender.sendMessage(TextUtil.prefixed(Component.text("Banned " + targetName + ". Reason: " + reason, NamedTextColor.GREEN)));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

}

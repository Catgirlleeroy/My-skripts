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

public class TempBanCommand implements BasicCommand {

    private final BanManager banManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private final PunishmentDiscordBroadcaster discordBroadcaster;
    private final PunishmentHistoryManager historyManager;

    public TempBanCommand(BanManager banManager, PlayerCache playerCache, JavaPlugin plugin,
                          VanishManager vanishManager, PunishmentDiscordBroadcaster discordBroadcaster,
                          PunishmentHistoryManager historyManager) {
        this.banManager         = banManager;
        this.playerCache        = playerCache;
        this.plugin             = plugin;
        this.vanishManager      = vanishManager;
        this.discordBroadcaster = discordBroadcaster;
        this.historyManager     = historyManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.tempban")) {
            sender.sendMessage(Component.text("You don't have permission to tempban players.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /tempban <player|uuid> <duration> [reason]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Duration examples: 30m, 2h, 1d, 1d12h", NamedTextColor.GRAY));
            return;
        }

        String input       = args[0];
        String durationStr = args[1];
        String reason = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "You have been temporarily banned.";

        long durationMs = BanManager.parseDuration(durationStr);
        if (durationMs == -1) {
            sender.sendMessage(Component.text("Invalid duration '" + durationStr + "'. Use formats like 30m, 2h, 1d.", NamedTextColor.RED));
            return;
        }

        UUID   uuid;
        String targetName;
        Player onlineTarget = Bukkit.getPlayerExact(input);

        if (onlineTarget != null) {
            uuid       = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            uuid = tryParseUUID(input);
            if (uuid == null) uuid = playerCache.getUUID(input);
            if (uuid == null) {
                sender.sendMessage(Component.text("Player '" + input + "' not found. They must have joined before, or provide a UUID.", NamedTextColor.RED));
                return;
            }
            String cached = playerCache.getName(uuid);
            targetName = cached != null ? cached : uuid.toString();
        }

        if (onlineTarget != null && (onlineTarget.hasPermission("bob.exempt") || onlineTarget.hasPermission("bob.exempt.tempban"))) {
            sender.sendMessage(Component.text(onlineTarget.getName() + " is exempt from this punishment.", NamedTextColor.RED));
            return;
        }

        if (banManager.isBanned(uuid)) {
            sender.sendMessage(Component.text(targetName + " is already banned.", NamedTextColor.RED));
            return;
        }

        banManager.tempBan(uuid, targetName, reason, sender.getName(), durationMs);
        historyManager.log(uuid, targetName, "tempban", reason, sender.getName(), durationStr);

        if (onlineTarget != null) {
            onlineTarget.getWorld().strikeLightningEffect(onlineTarget.getLocation());
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));
            final String finalReason   = reason;
            final long   finalDuration = durationMs;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    onlineTarget.kick(
                            Component.text("You have been temporarily banned.\n", NamedTextColor.RED)
                                    .append(Component.text("Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + finalDuration) + "\n", NamedTextColor.WHITE))
                                    .append(Component.text("Reason: " + finalReason, NamedTextColor.WHITE))
                    ), 10L);
        }

        TextUtil.broadcast("&c[TEMPBAN] &e" + targetName + " &chas been temporarily banned! &7Duration: " + durationStr + " | Reason: " + reason);
        discordBroadcaster.broadcast("tempban", targetName, sender.getName(), reason, durationStr);
        sender.sendMessage(Component.text("Tempbanned " + targetName + " for " + durationStr + ". Reason: " + reason, NamedTextColor.GREEN));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.onlinePlayers(stack, TabUtil.arg(args, 0), vanishManager);
        return java.util.Collections.emptyList();
    }

    private UUID tryParseUUID(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException e) { return null; }
    }
}

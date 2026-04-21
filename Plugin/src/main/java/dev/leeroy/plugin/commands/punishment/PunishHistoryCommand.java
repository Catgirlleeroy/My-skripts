package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.punishment.PunishmentHistoryManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PunishHistoryCommand implements BasicCommand {

    private static final int PAGE_SIZE = 5;

    private final PunishmentHistoryManager historyManager;
    private final PlayerCache playerCache;

    public PunishHistoryCommand(PunishmentHistoryManager historyManager, PlayerCache playerCache) {
        this.historyManager = historyManager;
        this.playerCache    = playerCache;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /history <player> [page]", NamedTextColor.YELLOW));
            return;
        }

        String input = args[0];
        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        UUID uuid;
        String targetName;

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            uuid       = online.getUniqueId();
            targetName = online.getName();
        } else {
            uuid = tryParseUUID(input);
            if (uuid == null) uuid = playerCache.getUUID(input);
            if (uuid == null) {
                sender.sendMessage(Component.text("Player '" + input + "' not found.", NamedTextColor.RED));
                return;
            }
            String cached = playerCache.getName(uuid);
            targetName = cached != null ? cached : input;
        }

        List<Map<String, Object>> history = historyManager.getHistory(uuid);

        if (history.isEmpty()) {
            sender.sendMessage(Component.text(targetName + " has no punishment history.", NamedTextColor.GRAY));
            return;
        }

        int totalPages = (int) Math.ceil(history.size() / (double) PAGE_SIZE);
        if (page > totalPages) page = totalPages;
        int start = (page - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, history.size());

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Punishment History: " + targetName + " (Page " + page + "/" + totalPages + ") ━━━", NamedTextColor.GOLD));

        for (int i = start; i < end; i++) {
            Map<String, Object> entry = history.get(i);
            int idx          = (int) entry.get("index");
            String type      = (String) entry.get("type");
            String reason    = (String) entry.get("reason");
            String punisher  = (String) entry.get("punisher");
            String duration  = (String) entry.get("duration");
            String timestamp = (String) entry.get("timestamp");

            NamedTextColor typeColor = switch (type.toLowerCase()) {
                case "ban", "tempban", "ipban", "tempipban" -> NamedTextColor.RED;
                case "mute", "tempmute" -> NamedTextColor.YELLOW;
                case "kick"             -> NamedTextColor.GOLD;
                case "warn"             -> NamedTextColor.AQUA;
                default                 -> NamedTextColor.GRAY;
            };

            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("#" + idx + " ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("[" + type.toUpperCase() + "]", typeColor))
                    .append(Component.text("  " + timestamp, NamedTextColor.DARK_GRAY)));
            sender.sendMessage(Component.text("  Reason: ", NamedTextColor.GRAY)
                    .append(Component.text(reason, NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("  Punisher: ", NamedTextColor.GRAY)
                    .append(Component.text(punisher, NamedTextColor.WHITE))
                    .append(Component.text("  Duration: ", NamedTextColor.GRAY))
                    .append(Component.text(duration, NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text(""));

        if (totalPages > 1) {
            Component prev = page > 1
                ? Component.text("◀ Prev", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/history " + targetName + " " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page - 1), NamedTextColor.GRAY)))
                : Component.text("◀ Prev", NamedTextColor.DARK_GRAY, TextDecoration.BOLD);

            Component next = page < totalPages
                ? Component.text("Next ▶", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/history " + targetName + " " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page + 1), NamedTextColor.GRAY)))
                : Component.text("Next ▶", NamedTextColor.DARK_GRAY, TextDecoration.BOLD);

            Component pageInfo = Component.text("  Page " + page + "/" + totalPages + "  ", NamedTextColor.GRAY);

            sender.sendMessage(Component.text("  ").append(prev).append(pageInfo).append(next));
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) return TabUtil.cachedPlayers(playerCache, TabUtil.arg(args, 0));
        return Collections.emptyList();
    }

    private UUID tryParseUUID(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException e) { return null; }
    }
}

package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.punishment.UnbanManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WhoUnbannedCommand implements BasicCommand {

    private static final int PAGE_SIZE = 5;

    private final UnbanManager unbanManager;
    private final PlayerCache playerCache;

    public WhoUnbannedCommand(UnbanManager unbanManager, PlayerCache playerCache) {
        this.unbanManager = unbanManager;
        this.playerCache  = playerCache;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /whounbanned <player> [page]", NamedTextColor.YELLOW));
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

        List<Map<String, Object>> history = unbanManager.getHistory(uuid);

        if (history.isEmpty()) {
            sender.sendMessage(Component.text(targetName + " has no unban history.", NamedTextColor.GRAY));
            return;
        }

        int totalPages = (int) Math.ceil(history.size() / (double) PAGE_SIZE);
        if (page > totalPages) page = totalPages;
        int start = (page - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, history.size());

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("━━━ Unban History: " + targetName + " (Page " + page + "/" + totalPages + ") ━━━", NamedTextColor.GREEN));

        for (int i = start; i < end; i++) {
            Map<String, Object> entry = history.get(i);
            int    idx        = (int)    entry.get("index");
            String unbannedBy = (String) entry.get("unbannedBy");
            String timestamp  = (String) entry.get("timestamp");

            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("#" + idx + " ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("[UNBAN]", NamedTextColor.GREEN))
                    .append(Component.text("  " + timestamp, NamedTextColor.DARK_GRAY)));
            sender.sendMessage(Component.text("  Unbanned by: ", NamedTextColor.GRAY)
                    .append(Component.text(unbannedBy, NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text(""));
        if (totalPages > 1) {
            sender.sendMessage(Component.text("Use /whounbanned " + targetName + " <page> to navigate.", NamedTextColor.DARK_GRAY));
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN));
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

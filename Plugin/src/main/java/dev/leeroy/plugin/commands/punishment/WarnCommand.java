package dev.leeroy.plugin.commands.punishment;

import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.punishment.WarnManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class WarnCommand implements CommandExecutor {

    private final WarnManager warnManager;
    private final PlayerCache playerCache;
    private final JavaPlugin plugin;
    private final String mode; // "warn", "unwarn", "warns"

    public WarnCommand(WarnManager warnManager, PlayerCache playerCache,
                       JavaPlugin plugin, String mode) {
        this.warnManager = warnManager;
        this.playerCache = playerCache;
        this.plugin      = plugin;
        this.mode        = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch (mode) {
            case "warn"   -> handleWarn(sender, args);
            case "unwarn" -> handleUnwarn(sender, args);
            case "warns"  -> handleWarns(sender, args);
        }
        return true;
    }

    // ── /warn <player> [reason] ───────────────────────────────────────────────

    private void handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bob.warn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to warn players.");
            return;
        }
        if (args.length < 1) { sender.sendMessage(ChatColor.YELLOW + "Usage: /warn <player> [reason]"); return; }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found."); return; }

        String targetName = getName(uuid, args[0]);
        String reason     = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        int max           = plugin.getConfig().getInt("warn.max-warns", 3);
        int warns         = warnManager.addWarn(uuid);

        // Sound to warned player if online
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            online.playSound(online.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        }

        // Broadcast
        String broadcastKey = reason != null ? "warned-broadcast" : "warned-broadcast-no-reason";
        String msg = color(plugin.getConfig().getString("warn.messages." + broadcastKey, ""))
                .replace("{player}", targetName)
                .replace("{staff}",  sender.getName())
                .replace("{reason}", reason != null ? reason : "")
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max));

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(color("&4&l                  WARNING!"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage("");

        // Auto-punish at max warns
        if (warns >= max) {
            final UUID fUUID = uuid;
            final String fName = targetName;
            final int offense = warnManager.getOffenses(uuid); // 0-indexed before increment
            warnManager.incrementOffenses(uuid);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (warnManager.getWarns(fUUID) >= max) {
                    if (!fName.matches("[a-zA-Z0-9_]{1,16}")) return;
                    java.util.List<String> punishments = plugin.getConfig()
                            .getStringList("warn.stacked-punishments");
                    String punishCmd;
                    if (punishments.isEmpty()) {
                        punishCmd = "tempban " + fName + " 1d Too many warnings!";
                    } else {
                        int index = Math.min(offense, punishments.size() - 1);
                        punishCmd = punishments.get(index).replace("{player}", fName);
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCmd);
                    if (plugin.getConfig().getBoolean("warn.reset-on-max", true)) {
                        warnManager.resetWarns(fUUID);
                    }
                }
            }, 40L);
        }
    }

    // ── /unwarn <player> ─────────────────────────────────────────────────────

    private void handleUnwarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bob.warn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return;
        }
        if (args.length < 1) { sender.sendMessage(ChatColor.YELLOW + "Usage: /unwarn <player>"); return; }

        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found."); return; }

        String targetName = getName(uuid, args[0]);
        int max           = plugin.getConfig().getInt("warn.max-warns", 3);

        if (warnManager.getWarns(uuid) <= 0) {
            sender.sendMessage(color(plugin.getConfig().getString("warn.messages.no-warns", "&4{player} &chas no warns.")
                    .replace("{player}", targetName)));
            return;
        }

        int warns = warnManager.removeWarn(uuid);

        String msg = color(plugin.getConfig().getString("warn.messages.unwarned-broadcast", ""))
                .replace("{player}", targetName)
                .replace("{staff}",  sender.getName())
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max));

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(color("&2&l                  UN-WARN!"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage("");
    }

    // ── /warns [player] ───────────────────────────────────────────────────────

    private void handleWarns(CommandSender sender, String[] args) {
        int max = plugin.getConfig().getInt("warn.max-warns", 3);

        if (args.length == 0 || !(sender instanceof Player)) {
            UUID uuid = sender instanceof Player p ? p.getUniqueId() : null;
            if (uuid == null) { sender.sendMessage(ChatColor.YELLOW + "Usage: /warns <player>"); return; }
            int warns = warnManager.getWarns(uuid);
            sender.sendMessage(color(plugin.getConfig().getString("warn.messages.check-self", "")
                    .replace("{warns}", String.valueOf(warns))
                    .replace("{max}",   String.valueOf(max))));
            return;
        }

        // /warns <player>
        UUID uuid = resolveUUID(args[0]);
        if (uuid == null) { sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found."); return; }

        // If checking self
        if (sender instanceof Player p && p.getUniqueId().equals(uuid)) {
            sender.sendMessage(color(plugin.getConfig().getString("warn.messages.check-self", "")
                    .replace("{warns}", String.valueOf(warnManager.getWarns(uuid)))
                    .replace("{max}",   String.valueOf(max))));
            return;
        }

        String targetName = getName(uuid, args[0]);
        int warns = warnManager.getWarns(uuid);
        sender.sendMessage(color(plugin.getConfig().getString("warn.messages.check-other", "")
                .replace("{player}", targetName)
                .replace("{warns}",  String.valueOf(warns))
                .replace("{max}",    String.valueOf(max))));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID resolveUUID(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online.getUniqueId();
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        return playerCache.getUUID(input);
    }

    private String getName(UUID uuid, String fallback) {
        String cached = playerCache.getName(uuid);
        return cached != null ? cached : fallback;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

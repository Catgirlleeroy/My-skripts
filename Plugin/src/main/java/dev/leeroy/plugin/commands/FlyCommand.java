package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.FlyConfig;
import dev.leeroy.plugin.Utils.FlyDataManager;
import dev.leeroy.plugin.Utils.FlyManager;
import dev.leeroy.plugin.Utils.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FlyCommand implements CommandExecutor {

    private final FlyManager flyManager;
    private final FlyDataManager flyData;
    private final FlyConfig flyConfig;
    private final PlayerCache playerCache;

    public FlyCommand(FlyManager flyManager, FlyDataManager flyData,
                      FlyConfig flyConfig, PlayerCache playerCache) {
        this.flyManager  = flyManager;
        this.flyData     = flyData;
        this.flyConfig   = flyConfig;
        this.playerCache = playerCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /fly — toggle own flight
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED + "Only players can toggle fly.");
                return true;
            }
            if (!p.hasPermission("bob.fly")) {
                p.sendMessage(flyConfig.msg("no-permission"));
                return true;
            }
            if (flyManager.isFlying(p.getUniqueId())) {
                flyManager.disableFly(p, "command");
                p.sendMessage(flyConfig.msg("fly-disabled"));
            } else {
                if (flyManager.enableFly(p)) {
                    p.sendMessage(flyConfig.msg("fly-enabled"));
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // /fly time [player]
            case "time" -> {
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player ? (Player) sender : null);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found or not online."); return true; }
                if (!sender.hasPermission("bob.fly") && !sender.getName().equals(target.getName())) {
                    sender.sendMessage(flyConfig.msg("no-permission")); return true;
                }
                if (flyData.hasPermanentFly(target.getUniqueId())) {
                    sender.sendMessage(flyConfig.msg("prefix") + FlyConfig.colorize("&f" + target.getName() + " &ahas &2permanent &aflight!"));
                } else {
                    long time = flyData.getTime(target.getUniqueId());
                    sender.sendMessage(flyConfig.msg("time-info").replace("{time}", FlyManager.formatTime(time)));
                }
            }

            // /fly give <player> <time>
            case "give" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(flyConfig.msg("no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /fly give <player> <time>"); return true; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(flyConfig.msg("player-not-found").replace("{player}", args[1])); return true; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(flyConfig.msg("invalid-time").replace("{time}", args[2])); return true; }
                flyData.addTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(flyConfig.msg("time-given-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds)));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(flyConfig.msg("time-given").replace("{time}", FlyManager.formatTime(seconds)));
            }

            // /fly set <player> <time>
            case "set" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(flyConfig.msg("no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /fly set <player> <time>"); return true; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(flyConfig.msg("player-not-found").replace("{player}", args[1])); return true; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(flyConfig.msg("invalid-time").replace("{time}", args[2])); return true; }
                flyData.setTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(flyConfig.msg("time-set-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds)));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(flyConfig.msg("time-set").replace("{time}", FlyManager.formatTime(seconds)));
            }

            // /fly remove <player> <time>
            case "remove" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(flyConfig.msg("no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /fly remove <player> <time>"); return true; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(flyConfig.msg("player-not-found").replace("{player}", args[1])); return true; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(flyConfig.msg("invalid-time").replace("{time}", args[2])); return true; }
                flyData.removeTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(flyConfig.msg("time-removed-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds)));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(flyConfig.msg("time-removed").replace("{time}", FlyManager.formatTime(seconds)));
            }

            // /fly perm <player> [true|false] — permanent fly
            case "perm" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(flyConfig.msg("no-permission")); return true; }
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /fly perm <player> [true|false]"); return true; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(flyConfig.msg("player-not-found").replace("{player}", args[1])); return true; }
                boolean value = args.length < 3 || !args[2].equalsIgnoreCase("false");
                flyData.setPermanentFly(uuid, value);
                String name = getName(uuid, args[1]);
                Player online = Bukkit.getPlayer(uuid);
                if (value) {
                    sender.sendMessage(FlyConfig.colorize("&a" + name + " now has &2permanent &aflight."));
                    if (online != null) { online.setAllowFlight(true); flyManager.enableFly(online); online.sendMessage(FlyConfig.colorize("&aYou now have &2permanent &aflight!")); }
                } else {
                    sender.sendMessage(FlyConfig.colorize("&cRemoved permanent flight from &f" + name + "&c."));
                    if (online != null) { flyData.setPermanentFly(uuid, false); flyManager.disableFly(online, "command"); online.sendMessage(FlyConfig.colorize("&cYour permanent flight was removed.")); }
                }
            }

            // /fly bypass — toggle flight bypass (no time drain)
            case "bypass" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (!p.hasPermission("bob.fly.bypass")) { p.sendMessage(flyConfig.msg("no-permission")); return true; }
                boolean current = flyData.hasPermanentFly(p.getUniqueId());
                flyData.setPermanentFly(p.getUniqueId(), !current);
                if (!current) {
                    p.setAllowFlight(true);
                    flyManager.enableFly(p);
                    p.sendMessage(FlyConfig.colorize(flyConfig.get().getString("messages.prefix","") + "&aFlight bypass &2enabled&a. No time will drain."));
                } else {
                    flyManager.disableFly(p, "command");
                    p.sendMessage(FlyConfig.colorize(flyConfig.get().getString("messages.prefix","") + "&cFlight bypass &4disabled&c."));
                }
            }

            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /fly [time|give|set|remove|perm|bypass]");
        }

        return true;
    }

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

    private long parseTime(String input) {
        long ms = BanManager.parseDuration(input);
        if (ms == -1) return -1;
        return ms / 1000;
    }
}
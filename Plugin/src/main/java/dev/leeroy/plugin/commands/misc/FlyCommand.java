package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.misc.FlyConfig;
import dev.leeroy.plugin.Utils.misc.FlyDataManager;
import dev.leeroy.plugin.Utils.misc.FlyManager;
import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TabUtil;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.VanishManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FlyCommand implements BasicCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList("time", "give", "set", "remove", "perm", "bypass");

    private final FlyManager flyManager;
    private final FlyDataManager flyData;
    private final FlyConfig flyConfig;
    private final PlayerCache playerCache;
    private final VanishManager vanishManager;

    public FlyCommand(FlyManager flyManager, FlyDataManager flyData,
                      FlyConfig flyConfig, PlayerCache playerCache, VanishManager vanishManager) {
        this.flyManager    = flyManager;
        this.flyData       = flyData;
        this.flyConfig     = flyConfig;
        this.playerCache   = playerCache;
        this.vanishManager = vanishManager;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            List<String> result = new java.util.ArrayList<>(TabUtil.onlinePlayers(stack, args[0], vanishManager));
            SUBCOMMANDS.stream().filter(s -> s.startsWith(lower)).forEach(result::add);
            return result;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("set") || sub.equals("remove") || sub.equals("perm") || sub.equals("time")) {
                return TabUtil.onlinePlayers(stack, args[1], vanishManager);
            }
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Only players can toggle fly.", NamedTextColor.RED));
                return;
            }
            if (!p.hasPermission("bob.fly")) {
                p.sendMessage(TextUtil.parse(flyConfig.msg("no-permission")));
                return;
            }
            if (flyManager.isFlying(p.getUniqueId())) {
                flyManager.disableFly(p, "command");
                p.sendMessage(TextUtil.parse(flyConfig.msg("fly-disabled")));
            } else {
                if (flyManager.enableFly(p)) {
                    p.sendMessage(TextUtil.parse(flyConfig.msg("fly-enabled")));
                }
            }
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            case "time" -> {
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player ? (Player) sender : null);
                if (target == null) { sender.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED)); return; }
                if (!sender.hasPermission("bob.fly") && !sender.getName().equals(target.getName())) {
                    sender.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return;
                }
                if (flyData.hasPermanentFly(target.getUniqueId())) {
                    sender.sendMessage(TextUtil.parse(flyConfig.prefix() + "&f" + target.getName() + " &ahas &2permanent &aflight!"));
                } else {
                    long time = flyData.getTime(target.getUniqueId());
                    sender.sendMessage(TextUtil.parse(flyConfig.msg("time-info").replace("{time}", FlyManager.formatTime(time))));
                }
            }

            case "give" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return; }
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /fly give <player> <time>", NamedTextColor.YELLOW)); return; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(TextUtil.parse(flyConfig.msg("player-not-found").replace("{player}", args[1]))); return; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(TextUtil.parse(flyConfig.msg("invalid-time").replace("{time}", args[2]))); return; }
                flyData.addTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(TextUtil.parse(flyConfig.msg("time-given-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds))));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(TextUtil.parse(flyConfig.msg("time-given").replace("{time}", FlyManager.formatTime(seconds))));
            }

            case "set" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return; }
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /fly set <player> <time>", NamedTextColor.YELLOW)); return; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(TextUtil.parse(flyConfig.msg("player-not-found").replace("{player}", args[1]))); return; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(TextUtil.parse(flyConfig.msg("invalid-time").replace("{time}", args[2]))); return; }
                flyData.setTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(TextUtil.parse(flyConfig.msg("time-set-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds))));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(TextUtil.parse(flyConfig.msg("time-set").replace("{time}", FlyManager.formatTime(seconds))));
            }

            case "remove" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return; }
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /fly remove <player> <time>", NamedTextColor.YELLOW)); return; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(TextUtil.parse(flyConfig.msg("player-not-found").replace("{player}", args[1]))); return; }
                long seconds = parseTime(args[2]);
                if (seconds == -1) { sender.sendMessage(TextUtil.parse(flyConfig.msg("invalid-time").replace("{time}", args[2]))); return; }
                flyData.removeTime(uuid, seconds);
                String name = getName(uuid, args[1]);
                sender.sendMessage(TextUtil.parse(flyConfig.msg("time-removed-other").replace("{player}", name).replace("{time}", FlyManager.formatTime(seconds))));
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(TextUtil.parse(flyConfig.msg("time-removed").replace("{time}", FlyManager.formatTime(seconds))));
            }

            case "perm" -> {
                if (!sender.hasPermission("bob.fly.admin")) { sender.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return; }
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /fly perm <player> [true|false]", NamedTextColor.YELLOW)); return; }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) { sender.sendMessage(TextUtil.parse(flyConfig.msg("player-not-found").replace("{player}", args[1]))); return; }
                boolean value = args.length < 3 || !args[2].equalsIgnoreCase("false");
                flyData.setPermanentFly(uuid, value);
                String name = getName(uuid, args[1]);
                Player online = Bukkit.getPlayer(uuid);
                if (value) {
                    sender.sendMessage(TextUtil.parse("&a" + name + " now has &2permanent &aflight."));
                    if (online != null) {
                        online.setAllowFlight(true);
                        flyManager.enableFly(online);
                        online.sendMessage(TextUtil.parse("&aYou now have &2permanent &aflight!"));
                    }
                } else {
                    sender.sendMessage(TextUtil.parse("&cRemoved permanent flight from &f" + name + "&c."));
                    if (online != null) {
                        flyData.setPermanentFly(uuid, false);
                        flyManager.disableFly(online, "command");
                        online.sendMessage(TextUtil.parse("&cYour permanent flight was removed."));
                    }
                }
            }

            case "bypass" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(Component.text("Players only.", NamedTextColor.RED)); return; }
                if (!p.hasPermission("bob.fly.bypass")) { p.sendMessage(TextUtil.parse(flyConfig.msg("no-permission"))); return; }
                boolean current = flyData.hasPermanentFly(p.getUniqueId());
                flyData.setPermanentFly(p.getUniqueId(), !current);
                if (!current) {
                    p.setAllowFlight(true);
                    flyManager.enableFly(p);
                    p.sendMessage(TextUtil.parse(flyConfig.prefix() + "&aFlight bypass &2enabled&a. No time will drain."));
                } else {
                    flyManager.disableFly(p, "command");
                    p.sendMessage(TextUtil.parse(flyConfig.prefix() + "&cFlight bypass &4disabled&c."));
                }
            }

            default -> sender.sendMessage(Component.text("Usage: /fly [time|give|set|remove|perm|bypass]", NamedTextColor.YELLOW));
        }
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

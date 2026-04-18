package dev.leeroy.plugin.commands.misc;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPACommand implements Listener {

    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, BukkitTask> warmupTasks = new HashMap<>();
    private final Map<UUID, Location> warmupLocations = new HashMap<>();

    private final JavaPlugin plugin;

    private static final long REQUEST_TIMEOUT_TICKS = 60 * 20L;

    public TPACommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── BasicCommand accessors ───────────────────────────────────────────────

    public BasicCommand tpa() {
        return (stack, args) -> {
            Player self = requirePlayer(stack);
            if (self != null) handleTPA(self, args);
        };
    }

    public BasicCommand tpaccept() {
        return (stack, args) -> {
            Player self = requirePlayer(stack);
            if (self != null) handleAccept(self);
        };
    }

    public BasicCommand tpdeny() {
        return (stack, args) -> {
            Player self = requirePlayer(stack);
            if (self != null) handleDeny(self);
        };
    }

    private Player requirePlayer(CommandSourceStack stack) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return null;
        }
        return player;
    }

    // ── /tpa <player> ────────────────────────────────────────────────────────

    private void handleTPA(Player sender, String[] args) {
        if (!sender.hasPermission("bob.tpa")) {
            sender.sendMessage(Component.text("You don't have permission to send teleport requests.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /tpa <player>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(Component.text("You can't send a teleport request to yourself.", NamedTextColor.RED));
            return;
        }

        pendingRequests.put(target.getUniqueId(), sender.getUniqueId());

        sender.sendMessage(Component.text("Teleport request sent to " + target.getName() + ". Expires in 60 seconds.", NamedTextColor.GREEN));
        target.sendMessage(Component.text(sender.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" wants to teleport to you. Type ", NamedTextColor.WHITE))
                .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                .append(Component.text(" or ", NamedTextColor.WHITE))
                .append(Component.text("/tpdeny", NamedTextColor.RED))
                .append(Component.text(".", NamedTextColor.WHITE)));

        final UUID targetId = target.getUniqueId();
        final UUID senderId = sender.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.getOrDefault(targetId, null) != null &&
                    pendingRequests.get(targetId).equals(senderId)) {
                pendingRequests.remove(targetId);
                Player t = Bukkit.getPlayer(targetId);
                Player s = Bukkit.getPlayer(senderId);
                if (s != null) s.sendMessage(Component.text("Your teleport request to "
                        + (t != null ? t.getName() : "that player") + " expired.", NamedTextColor.RED));
                if (t != null) t.sendMessage(Component.text("Teleport request from "
                        + (s != null ? s.getName() : "a player") + " expired.", NamedTextColor.GRAY));
            }
        }, REQUEST_TIMEOUT_TICKS);
    }

    // ── /tpaccept ────────────────────────────────────────────────────────────

    private void handleAccept(Player target) {
        if (!target.hasPermission("bob.tpa")) {
            target.sendMessage(Component.text("You don't have permission to accept teleport requests.", NamedTextColor.RED));
            return;
        }

        UUID requesterID = pendingRequests.remove(target.getUniqueId());
        if (requesterID == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(requesterID);
        if (requester == null) {
            target.sendMessage(Component.text("The player who requested teleport is no longer online.", NamedTextColor.RED));
            return;
        }

        int delaySecs = plugin.getConfig().getInt("teleport.tpa-delay", 3);

        if (delaySecs <= 0) {
            requester.teleport(target.getLocation());
            requester.sendMessage(Component.text("Teleporting to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Accepted teleport request from " + requester.getName() + ".", NamedTextColor.GREEN));
            return;
        }

        requester.sendMessage(Component.text("Teleporting to " + target.getName()
                + " in " + delaySecs + " seconds. Don't move or take damage!", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Accepted teleport request from " + requester.getName() + ".", NamedTextColor.GREEN));

        warmupLocations.put(requester.getUniqueId(), requester.getLocation().clone());

        final UUID targetId    = target.getUniqueId();
        final UUID requesterId = requester.getUniqueId();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            warmupTasks.remove(requesterId);
            warmupLocations.remove(requesterId);

            Player r = Bukkit.getPlayer(requesterId);
            Player t = Bukkit.getPlayer(targetId);

            if (r == null || t == null) return;

            r.teleport(t.getLocation());
            r.sendMessage(Component.text("Teleported to " + t.getName() + "!", NamedTextColor.GREEN));
        }, delaySecs * 20L);

        warmupTasks.put(requester.getUniqueId(), task);
    }

    // ── /tpdeny ──────────────────────────────────────────────────────────────

    private void handleDeny(Player target) {
        UUID requesterID = pendingRequests.remove(target.getUniqueId());
        if (requesterID == null) {
            target.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(requesterID);
        target.sendMessage(Component.text("Teleport request denied.", NamedTextColor.RED));
        if (requester != null) {
            requester.sendMessage(Component.text(target.getName() + " denied your teleport request.", NamedTextColor.RED));
        }
    }

    // ── Cancel warmup helper ─────────────────────────────────────────────────

    private void cancelWarmup(Player player, String reason) {
        BukkitTask task = warmupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            warmupLocations.remove(player.getUniqueId());
            player.sendMessage(Component.text("Teleport cancelled: " + reason, NamedTextColor.RED));
        }
    }

    // ── Listeners ────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getConfig().getBoolean("teleport.tpa-cancel-on-damage", true)) return;
        if (!warmupTasks.containsKey(player.getUniqueId())) return;

        cancelWarmup(player, "you took damage.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!warmupTasks.containsKey(player.getUniqueId())) return;

        double maxMove = plugin.getConfig().getDouble("teleport.tpa-cancel-on-move", 1.0);
        if (maxMove <= 0) return;

        Location origin = warmupLocations.get(player.getUniqueId());
        if (origin == null) return;

        Location current = event.getTo();
        if (current == null) return;

        double dist = Math.sqrt(
                Math.pow(current.getX() - origin.getX(), 2) +
                        Math.pow(current.getY() - origin.getY(), 2) +
                        Math.pow(current.getZ() - origin.getZ(), 2)
        );

        if (dist > maxMove) {
            cancelWarmup(player, "you moved.");
        }
    }
}

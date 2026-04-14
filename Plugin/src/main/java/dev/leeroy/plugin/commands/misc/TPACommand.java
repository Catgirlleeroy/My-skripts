package dev.leeroy.plugin.commands.misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
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

public class TPACommand implements CommandExecutor, Listener {

    // Maps target UUID → requester UUID (pending requests)
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    // Maps requester UUID → warmup task (so we can cancel it)
    private final Map<UUID, BukkitTask> warmupTasks = new HashMap<>();

    // Maps requester UUID → location at warmup start (for movement check)
    private final Map<UUID, Location> warmupLocations = new HashMap<>();

    private final JavaPlugin plugin;

    private static final long REQUEST_TIMEOUT_TICKS = 60 * 20L;

    public TPACommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player self = (Player) sender;

        return switch (label.toLowerCase()) {
            case "tpa"       -> handleTPA(self, args);
            case "tpaccept"  -> handleAccept(self);
            case "tpdeny"    -> handleDeny(self);
            default          -> false;
        };
    }

    // ── /tpa <player> ────────────────────────────────────────────────────────

    private boolean handleTPA(Player sender, String[] args) {
        if (!sender.hasPermission("bob.tpa")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to send teleport requests.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.RED + "You can't send a teleport request to yourself.");
            return true;
        }

        pendingRequests.put(target.getUniqueId(), sender.getUniqueId());

        int delay = plugin.getConfig().getInt("teleport.tpa-delay", 3);

        sender.sendMessage(ChatColor.GREEN + "Teleport request sent to " + target.getName() + ". Expires in 60 seconds.");
        target.sendMessage(
                ChatColor.YELLOW + sender.getName() + ChatColor.WHITE + " wants to teleport to you. Type " +
                        ChatColor.GREEN + "/tpaccept" + ChatColor.WHITE + " or " +
                        ChatColor.RED + "/tpdeny" + ChatColor.WHITE + "."
        );

        // Auto-expire after 60 seconds
        final UUID targetId  = target.getUniqueId();
        final UUID senderId  = sender.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.getOrDefault(targetId, null) != null &&
                    pendingRequests.get(targetId).equals(senderId)) {
                pendingRequests.remove(targetId);
                Player t = Bukkit.getPlayer(targetId);
                Player s = Bukkit.getPlayer(senderId);
                if (s != null) s.sendMessage(ChatColor.RED + "Your teleport request to "
                        + (t != null ? t.getName() : "that player") + " expired.");
                if (t != null) t.sendMessage(ChatColor.GRAY + "Teleport request from "
                        + (s != null ? s.getName() : "a player") + " expired.");
            }
        }, REQUEST_TIMEOUT_TICKS);

        return true;
    }

    // ── /tpaccept ────────────────────────────────────────────────────────────

    private boolean handleAccept(Player target) {
        if (!target.hasPermission("bob.tpa")) {
            target.sendMessage(ChatColor.RED + "You don't have permission to accept teleport requests.");
            return true;
        }

        UUID requesterID = pendingRequests.remove(target.getUniqueId());
        if (requesterID == null) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterID);
        if (requester == null) {
            target.sendMessage(ChatColor.RED + "The player who requested teleport is no longer online.");
            return true;
        }

        int delaySecs = plugin.getConfig().getInt("teleport.tpa-delay", 3);

        if (delaySecs <= 0) {
            // Instant teleport
            requester.teleport(target.getLocation());
            requester.sendMessage(ChatColor.GREEN + "Teleporting to " + target.getName() + "!");
            target.sendMessage(ChatColor.GREEN + "Accepted teleport request from " + requester.getName() + ".");
            return true;
        }

        // Warmup
        requester.sendMessage(ChatColor.GREEN + "Teleporting to " + target.getName()
                + " in " + delaySecs + " seconds. Don't move or take damage!");
        target.sendMessage(ChatColor.GREEN + "Accepted teleport request from " + requester.getName() + ".");

        // Store starting location for movement check
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
            r.sendMessage(ChatColor.GREEN + "Teleported to " + t.getName() + "!");
        }, delaySecs * 20L);

        warmupTasks.put(requester.getUniqueId(), task);
        return true;
    }

    // ── /tpdeny ──────────────────────────────────────────────────────────────

    private boolean handleDeny(Player target) {
        UUID requesterID = pendingRequests.remove(target.getUniqueId());
        if (requesterID == null) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterID);
        target.sendMessage(ChatColor.RED + "Teleport request denied.");
        if (requester != null) {
            requester.sendMessage(ChatColor.RED + target.getName() + " denied your teleport request.");
        }
        return true;
    }

    // ── Cancel warmup helper ─────────────────────────────────────────────────

    private void cancelWarmup(Player player, String reason) {
        BukkitTask task = warmupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            warmupLocations.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Teleport cancelled: " + reason);
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

        // Only care about X/Z movement, not looking around
        Location current = event.getTo();
        if (current == null) return;

        double dist = Math.sqrt(
                Math.pow(current.getX() - origin.getX(), 2) +
                        Math.pow(current.getZ() - origin.getZ(), 2)
        );

        if (dist > maxMove) {
            cancelWarmup(player, "you moved.");
        }
    }
}
package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final CombatManager combatManager;
    private final JavaPlugin plugin;

    // Cooldown per player per message type — stores last sent time in ms
    private final Map<UUID, Long> regionMsgCooldown  = new HashMap<>();
    private final Map<UUID, Long> fireworkMsgCooldown = new HashMap<>();
    private final Map<UUID, Long> commandMsgCooldown  = new HashMap<>();

    private static final long MSG_COOLDOWN_MS = 3000; // 3 seconds between same message

    public CombatListener(CombatManager combatManager, JavaPlugin plugin) {
        this.combatManager = combatManager;
        this.plugin        = plugin;
    }

    // ── PvP — tag both players ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tag.enabled", true)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        boolean attackerWasTagged = combatManager.isTagged(attacker.getUniqueId());
        boolean victimWasTagged   = combatManager.isTagged(victim.getUniqueId());

        combatManager.tag(attacker);
        combatManager.tag(victim);

        if (!attackerWasTagged) {
            attacker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("combat-tag.messages.tagged-attacker", "")
                            .replace("{victim}", victim.getName())));
        }
        if (!victimWasTagged) {
            victim.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("combat-tag.messages.tagged-victim", "")
                            .replace("{attacker}", attacker.getName())));
        }
    }

    // ── Death — remove tag ────────────────────────────────────────────────────

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        combatManager.untag(event.getEntity());
    }

    // ── Quit — combat log ─────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player.getUniqueId())) return;

        combatManager.untag(player);

        if (plugin.getConfig().getBoolean("combat-tag.broadcast-combatlog", true)) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("combat-tag.messages.combatlog", "&c{player} &7just combat logged!")
                            .replace("{player}", player.getName()));
            Bukkit.broadcastMessage(msg);
        }
    }

    // ── Commands — block while tagged ────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tag.enabled", true)) return;
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase().substring(1);
        List<String> blocked = plugin.getConfig().getStringList("combat-tag.blocked-commands");

        for (String b : blocked) {
            if (cmd.startsWith(b.toLowerCase()) || cmd.contains(b.toLowerCase())) {
                event.setCancelled(true);
                if (canSend(commandMsgCooldown, player.getUniqueId())) {
                    player.sendMessage(combatManager.msg("blocked-command"));
                }
                return;
            }
        }
    }

    // ── Region — block entry while tagged ────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tag.enabled", true)) return;
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player.getUniqueId())) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;

        List<String> blockedRegions = plugin.getConfig().getStringList("combat-tag.blocked-regions");
        if (blockedRegions.isEmpty()) return;

        try {
            com.sk89q.worldguard.WorldGuard wg = com.sk89q.worldguard.WorldGuard.getInstance();
            com.sk89q.worldguard.protection.regions.RegionContainer container = wg.getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();

            com.sk89q.worldedit.util.Location weLoc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(event.getTo());
            com.sk89q.worldguard.protection.ApplicableRegionSet regions = query.getApplicableRegions(weLoc);

            for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regions) {
                for (String blocked : blockedRegions) {
                    if (region.getId().toLowerCase().contains(blocked.toLowerCase())) {
                        event.setCancelled(true);
                        player.teleport(event.getFrom());
                        if (canSend(regionMsgCooldown, player.getUniqueId())) {
                            player.sendMessage(combatManager.msg("blocked-region"));
                        }
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Fireworks — block while tagged ───────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tag.enabled", true)) return;
        Player player = event.getPlayer();
        if (!combatManager.isTagged(player.getUniqueId())) return;

        if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
            if (canSend(fireworkMsgCooldown, player.getUniqueId())) {
                player.sendMessage(combatManager.msg("firework-blocked"));
            }
        }
    }

    // ── Cooldown helper ───────────────────────────────────────────────────────

    private boolean canSend(Map<UUID, Long> cooldownMap, UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = cooldownMap.get(uuid);
        if (last == null || now - last >= MSG_COOLDOWN_MS) {
            cooldownMap.put(uuid, now);
            return true;
        }
        return false;
    }
}
package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.FlyConfig;
import dev.leeroy.plugin.Utils.FlyDataManager;
import dev.leeroy.plugin.Utils.FlyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FlyListener implements Listener {

    private final FlyManager flyManager;
    private final FlyDataManager flyData;
    private final FlyConfig flyConfig;

    public FlyListener(FlyManager flyManager, FlyDataManager flyData, FlyConfig flyConfig) {
        this.flyManager = flyManager;
        this.flyData    = flyData;
        this.flyConfig  = flyConfig;
    }

    // ── Join — daily bonus + restore flight ───────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // First join bonus
        if (!flyData.hasJoinedBefore(p.getUniqueId())) {
            flyData.markFirstJoin(p.getUniqueId());
            long bonus = flyConfig.get().getLong("general.bonus.first-join", 0L);
            if (bonus > 0) {
                flyData.addTime(p.getUniqueId(), bonus);
                p.sendMessage(flyConfig.msg("first-join").replace("{time}", FlyManager.formatTime(bonus)));
            }
        }

        // Daily bonus
        if (!flyData.hasReceivedDailyBonus(p.getUniqueId())) {
            flyData.markDailyBonus(p.getUniqueId());
            long totalBonus = 0;

            // Check permission groups
            if (flyConfig.get().isConfigurationSection("general.bonus.daily-login")) {
                for (String group : flyConfig.get().getConfigurationSection("general.bonus.daily-login").getKeys(false)) {
                    if (p.hasPermission("bob.fly.bonus." + group)) {
                        totalBonus += flyConfig.get().getLong("general.bonus.daily-login." + group, 0L);
                    }
                }
            }

            if (totalBonus > 0) {
                flyData.addTime(p.getUniqueId(), totalBonus);
                p.sendMessage(flyConfig.msg("daily-bonus").replace("{time}", FlyManager.formatTime(totalBonus)));
            }
        }

        // Restore permanent fly
        if (flyData.hasPermanentFly(p.getUniqueId())) {
            p.setAllowFlight(true);
        }
    }

    // ── Quit — clean up ───────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flyManager.cleanup(event.getPlayer().getUniqueId());
    }

    // ── Movement — track for idle + update action bar ─────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        // Only count actual position change, not just head rotation
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        flyManager.updateLastMoved(p.getUniqueId());

        // Update action bar while flying
        if (flyManager.isFlying(p.getUniqueId())) {
            flyManager.updateActionBar(p);
        }
    }

    // ── Combat — disable flight on hit ────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Player attacks another player
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            if (flyConfig.get().getBoolean("general.combat.attack-player", true)) {
                flyManager.onCombat(attacker);
            }
            if (flyConfig.get().getBoolean("general.combat.attacked-by-player", true)) {
                flyManager.onCombat(victim);
            }
            return;
        }

        // Player attacks a mob
        if (event.getDamager() instanceof Player attacker) {
            if (flyConfig.get().getBoolean("general.combat.attack-mob", false)) {
                flyManager.onCombat(attacker);
            }
        }

        // Mob attacks a player
        if (event.getEntity() instanceof Player victim) {
            if (flyConfig.get().getBoolean("general.combat.attacked-by-mob", false)) {
                flyManager.onCombat(victim);
            }
        }
    }
}
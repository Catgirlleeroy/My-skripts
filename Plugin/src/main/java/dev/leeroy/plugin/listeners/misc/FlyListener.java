package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.misc.FlyConfig;
import dev.leeroy.plugin.Utils.misc.FlyDataManager;
import dev.leeroy.plugin.Utils.misc.FlyManager;
import dev.leeroy.plugin.Utils.misc.TextUtil;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        if (!flyData.hasJoinedBefore(p.getUniqueId())) {
            flyData.markFirstJoin(p.getUniqueId());
            long bonus = flyConfig.get().getLong("general.bonus.first-join", 0L);
            if (bonus > 0) {
                flyData.addTime(p.getUniqueId(), bonus);
                p.sendMessage(TextUtil.parse(flyConfig.msg("first-join").replace("{time}", FlyManager.formatTime(bonus))));
            }
        }

        if (!flyData.hasReceivedDailyBonus(p.getUniqueId())) {
            flyData.markDailyBonus(p.getUniqueId());
            long totalBonus = 0;

            if (flyConfig.get().isConfigurationSection("general.bonus.daily-login")) {
                for (String group : flyConfig.get().getConfigurationSection("general.bonus.daily-login").getKeys(false)) {
                    if (p.hasPermission("bob.fly.bonus." + group)) {
                        totalBonus += flyConfig.get().getLong("general.bonus.daily-login." + group, 0L);
                    }
                }
            }

            if (totalBonus > 0) {
                flyData.addTime(p.getUniqueId(), totalBonus);
                p.sendMessage(TextUtil.parse(flyConfig.msg("daily-bonus").replace("{time}", FlyManager.formatTime(totalBonus))));
            }
        }

        if (flyData.hasPermanentFly(p.getUniqueId())) {
            p.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flyManager.cleanup(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        flyManager.updateLastMoved(p.getUniqueId());

        if (flyManager.isFlying(p.getUniqueId())) {
            flyManager.updateActionBar(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            if (flyConfig.get().getBoolean("general.combat.attack-player", true)) {
                flyManager.onCombat(attacker);
            }
            if (flyConfig.get().getBoolean("general.combat.attacked-by-player", true)) {
                flyManager.onCombat(victim);
            }
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            if (flyConfig.get().getBoolean("general.combat.attack-mob", false)) {
                flyManager.onCombat(attacker);
            }
        }

        if (event.getEntity() instanceof Player victim) {
            if (flyConfig.get().getBoolean("general.combat.attacked-by-mob", false)) {
                flyManager.onCombat(victim);
            }
        }
    }
}

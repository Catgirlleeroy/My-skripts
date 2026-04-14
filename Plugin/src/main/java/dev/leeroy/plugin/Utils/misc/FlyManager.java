package dev.leeroy.plugin.Utils.misc;

import dev.leeroy.plugin.Utils.combat.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FlyManager {

    private final JavaPlugin plugin;
    private final FlyDataManager dataManager;
    private final FlyConfig flyConfig;
    private CombatManager combatManager; // set after construction to avoid circular dependency

    // Players currently flying with tempfly
    private final Set<UUID> activeFlyers = new HashSet<>();
    // Idle tracking — last moved time
    private final Map<UUID, Long> lastMoved = new HashMap<>();
    // The main timer task
    private BukkitTask timerTask;

    public FlyManager(JavaPlugin plugin, FlyDataManager dataManager, FlyConfig flyConfig) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
        this.flyConfig   = flyConfig;
        startTimer();
    }

    public void setCombatManager(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void startTimer() {
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(activeFlyers)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) { activeFlyers.remove(uuid); continue; }

                // Permanent fly — no drain
                if (dataManager.hasPermanentFly(uuid)) {
                    updateActionBar(p);
                    continue;
                }

                // Skip drain if on ground and ground timer is off
                boolean onGround = p.isOnGround();
                boolean groundDrain = flyConfig.get().getBoolean("general.timer.ground", false);
                if (onGround && !groundDrain) {
                    updateActionBar(p);
                    continue;
                }

                // Skip drain if in creative and creative timer is off
                if (p.getGameMode() == org.bukkit.GameMode.CREATIVE &&
                        !flyConfig.get().getBoolean("general.timer.creative", false)) {
                    updateActionBar(p);
                    continue;
                }

                // Check idle
                int idleThreshold = flyConfig.get().getInt("general.idle.threshold", -1);
                if (idleThreshold > 0) {
                    long lastMove = lastMoved.getOrDefault(uuid, System.currentTimeMillis());
                    if ((System.currentTimeMillis() - lastMove) / 1000 >= idleThreshold) {
                        if (flyConfig.get().getBoolean("general.idle.drop-on-idle", false)) {
                            disableFly(p, "idle");
                            p.sendMessage(msg("idle-drop"));
                        }
                        continue;
                    }
                }

                // Drain 1 second
                long time = dataManager.getTime(uuid);
                if (time <= 0) {
                    disableFly(p, "out-of-time");
                    p.sendMessage(msg("out-of-time"));
                    continue;
                }

                dataManager.removeTime(uuid, 1);
                long remaining = time - 1;

                // Warning thresholds
                List<Integer> thresholds = flyConfig.get().getIntegerList("aesthetic.warning.thresholds");
                if (flyConfig.get().getBoolean("aesthetic.warning.enabled", true) && thresholds.contains((int) remaining)) {
                    sendWarning(p, remaining);
                }

                updateActionBar(p);
            }

            // Constant timer — drain even when not flying
            if (flyConfig.get().getBoolean("general.timer.constant", false)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (activeFlyers.contains(p.getUniqueId())) continue;
                    if (dataManager.hasPermanentFly(p.getUniqueId())) continue;
                    long time = dataManager.getTime(p.getUniqueId());
                    if (time > 0) dataManager.removeTime(p.getUniqueId(), 1);
                }
            }

        }, 20L, 20L); // every second
    }

    // ── Flight control ────────────────────────────────────────────────────────

    public boolean enableFly(Player p) {
        // Use CombatManager if available, else skip check
        if (combatManager != null && combatManager.isTagged(p.getUniqueId())) {
            p.sendMessage(msg("combat-tagged"));
            return false;
        }

        boolean permanent = dataManager.hasPermanentFly(p.getUniqueId());
        long time = dataManager.getTime(p.getUniqueId());

        if (!permanent && time <= 0) {
            p.sendMessage(msg("no-time"));
            return false;
        }

        int maxHeight = flyConfig.get().getInt("general.flight.max-height", -1);
        if (maxHeight > 0 && p.getLocation().getY() > maxHeight) {
            p.sendMessage(FlyConfig.colorize(flyConfig.get().getString("messages.prefix", "") +
                    "&cYou cannot fly above Y " + maxHeight + "!"));
            return false;
        }

        activeFlyers.add(p.getUniqueId());
        p.setAllowFlight(true);
        p.setFlying(true);

        float speed = (float) flyConfig.get().getDouble("general.flight.speed.default", 1.0);
        p.setFlySpeed(Math.min(speed * 0.1f, 1.0f));

        updateActionBar(p);
        return true;
    }

    public void disableFly(Player p, String reason) {
        activeFlyers.remove(p.getUniqueId());

        boolean takeDamage = switch (reason) {
            case "out-of-time" -> flyConfig.get().getBoolean("general.damage.out-of-time", true);
            case "combat"      -> flyConfig.get().getBoolean("general.damage.combat", true);
            case "idle"        -> false;
            default            -> flyConfig.get().getBoolean("general.damage.on-command", false);
        };

        if (!takeDamage) p.setFallDistance(0);

        if (!dataManager.hasPermanentFly(p.getUniqueId())) {
            p.setAllowFlight(false);
        }
        p.setFlying(false);
        clearActionBar(p);
    }

    public boolean isFlying(UUID uuid) {
        return activeFlyers.contains(uuid);
    }

    public void updateLastMoved(UUID uuid) {
        lastMoved.put(uuid, System.currentTimeMillis());
    }

    // ── Combat — delegated to CombatManager ──────────────────────────────────

    /** Called by FlyListener when a player takes damage — disables fly if active */
    public void onCombat(Player p) {
        if (isFlying(p.getUniqueId())) {
            disableFly(p, "combat");
            p.sendMessage(msg("combat-tagged"));
        }
    }

    // ── Action bar ────────────────────────────────────────────────────────────

    public void updateActionBar(Player p) {
        if (!flyConfig.get().getBoolean("aesthetic.action-bar.enabled", true)) return;

        UUID uuid = p.getUniqueId();
        boolean permanent = dataManager.hasPermanentFly(uuid);
        long time = dataManager.getTime(uuid);

        String text;
        if (permanent) {
            text = FlyConfig.colorize(flyConfig.get().getString("aesthetic.action-bar.text", "&6✈ Flight&7: &f{time}")
                    .replace("{time}", "&a∞ Permanent"));
        } else if (time <= 0) {
            text = FlyConfig.colorize(flyConfig.get().getString("aesthetic.action-bar.no-time-text",
                    "&6✈ Flight&7: &cNo time remaining"));
        } else {
            text = FlyConfig.colorize(flyConfig.get().getString("aesthetic.action-bar.text", "&6✈ Flight&7: &f{time}")
                    .replace("{time}", formatTime(time)));
        }

        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
    }

    public void clearActionBar(Player p) {
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
    }

    // ── Warning subtitle ──────────────────────────────────────────────────────

    private void sendWarning(Player p, long remaining) {
        String title    = FlyConfig.colorize(flyConfig.get().getString("aesthetic.warning.title", "&cWARNING!"));
        String subtitle = FlyConfig.colorize(flyConfig.get().getString("aesthetic.warning.subtitle",
                        "&fYou have &e{time} &fof flight remaining!")
                .replace("{time}", formatTime(remaining)));
        p.sendTitle(title, subtitle, 5, 40, 10);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static String formatTime(long totalSeconds) {
        long days    = totalSeconds / 86400; totalSeconds %= 86400;
        long hours   = totalSeconds / 3600;  totalSeconds %= 3600;
        long minutes = totalSeconds / 60;    totalSeconds %= 60;
        long seconds = totalSeconds;

        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private String msg(String key) {
        return flyConfig.msg(key);
    }

    public void cleanup(UUID uuid) {
        activeFlyers.remove(uuid);
        lastMoved.remove(uuid);
    }
}
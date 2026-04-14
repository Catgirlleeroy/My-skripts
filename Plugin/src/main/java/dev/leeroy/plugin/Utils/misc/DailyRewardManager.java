package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DailyRewardManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public DailyRewardManager(JavaPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "dailyreward.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /** Records the current timestamp as the last claim time. */
    public void recordClaim(UUID uuid) {
        int cooldownHours = plugin.getConfig().getInt("daily-reward.cooldown-hours", 24);
        long nextClaimAt  = System.currentTimeMillis() + (cooldownHours * 3600_000L);
        config.set(uuid.toString(), nextClaimAt);
        save();
    }

    /** Returns true if the player can claim (never claimed or cooldown expired). */
    public boolean canClaim(UUID uuid) {
        if (!config.contains(uuid.toString())) return true;
        return System.currentTimeMillis() >= config.getLong(uuid.toString());
    }

    /**
     * Returns remaining time as int[]{hours, minutes}.
     * Returns {0, 0} if they can already claim.
     */
    public int[] getTimeRemaining(UUID uuid) {
        if (canClaim(uuid)) return new int[]{0, 0};
        long msLeft      = config.getLong(uuid.toString()) - System.currentTimeMillis();
        long totalMinutes = msLeft / 60_000L;
        int hours   = (int)(totalMinutes / 60);
        int minutes = (int)(totalMinutes % 60);
        return new int[]{hours, minutes};
    }

    public void resetAll() {
        for (String key : config.getKeys(false)) {
            config.set(key, 0L);
        }
        save();
    }

    public void initPlayer(UUID uuid) {
        // Nothing needed — canClaim returns true if key is absent
    }

    public JavaPlugin plugin() { return plugin; }
}
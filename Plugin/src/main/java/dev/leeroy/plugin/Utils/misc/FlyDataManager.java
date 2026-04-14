package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FlyDataManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public FlyDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flydata.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::load);
    }

    private void save() {
        final YamlConfiguration snapshot = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { snapshot.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    private String key(UUID uuid) { return "players." + uuid.toString(); }

    // ── Time ─────────────────────────────────────────────────────────────────

    public long getTime(UUID uuid) {
        return config.getLong(key(uuid) + ".time", 0L);
    }

    public void setTime(UUID uuid, long seconds) {
        config.set(key(uuid) + ".time", Math.max(0, seconds));
        save();
    }

    public void addTime(UUID uuid, long seconds) {
        setTime(uuid, getTime(uuid) + seconds);
    }

    public void removeTime(UUID uuid, long seconds) {
        setTime(uuid, Math.max(0, getTime(uuid) - seconds));
    }

    // ── Permanent fly ─────────────────────────────────────────────────────────

    public boolean hasPermanentFly(UUID uuid) {
        return config.getBoolean(key(uuid) + ".permanent", false);
    }

    public void setPermanentFly(UUID uuid, boolean value) {
        config.set(key(uuid) + ".permanent", value);
        save();
    }

    // ── Daily reset ───────────────────────────────────────────────────────────

    public String getLastLoginDate(UUID uuid) {
        return config.getString(key(uuid) + ".last-login", "");
    }

    public void setLastLoginDate(UUID uuid, String date) {
        config.set(key(uuid) + ".last-login", date);
        save();
    }

    public boolean hasReceivedDailyBonus(UUID uuid) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return today.equals(getLastLoginDate(uuid));
    }

    public void markDailyBonus(UUID uuid) {
        setLastLoginDate(uuid, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
    }

    // ── First join ────────────────────────────────────────────────────────────

    public boolean hasJoinedBefore(UUID uuid) {
        return config.contains(key(uuid));
    }

    public void markFirstJoin(UUID uuid) {
        config.set(key(uuid) + ".first-join", true);
        save();
    }
}
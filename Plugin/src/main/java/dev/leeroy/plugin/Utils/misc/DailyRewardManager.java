package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;

public class DailyRewardManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public DailyRewardManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;
    }

    public void reload() { /* H2 is always current */ }

    public void recordClaim(UUID uuid) {
        int cooldownHours = plugin.getConfig().getInt("daily-reward.cooldown-hours", 24);
        long nextClaimAt  = System.currentTimeMillis() + (cooldownHours * 3600_000L);
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO daily_reward (uuid, next_claim_at) KEY (uuid) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, nextClaimAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[DailyRewardManager] recordClaim failed: " + e.getMessage());
        }
    }

    public boolean canClaim(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT next_claim_at FROM daily_reward WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return true;
                return System.currentTimeMillis() >= rs.getLong("next_claim_at");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DailyRewardManager] canClaim failed: " + e.getMessage());
            return true;
        }
    }

    public int[] getTimeRemaining(UUID uuid) {
        if (canClaim(uuid)) return new int[]{0, 0};
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT next_claim_at FROM daily_reward WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new int[]{0, 0};
                long msLeft       = rs.getLong("next_claim_at") - System.currentTimeMillis();
                long totalMinutes = msLeft / 60_000L;
                int hours   = (int)(totalMinutes / 60);
                int minutes = (int)(totalMinutes % 60);
                return new int[]{hours, minutes};
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[DailyRewardManager] getTimeRemaining failed: " + e.getMessage());
            return new int[]{0, 0};
        }
    }

    public void resetAll() {
        try (Connection c = db.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE daily_reward SET next_claim_at = 0");
        } catch (SQLException e) {
            plugin.getLogger().warning("[DailyRewardManager] resetAll failed: " + e.getMessage());
        }
    }

    public void initPlayer(UUID uuid) { /* canClaim returns true if row is absent */ }

    public JavaPlugin plugin() { return plugin; }
}

package dev.leeroy.plugin.Utils.misc;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class DatabaseManager {

    private final String url;

    public DatabaseManager(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        String path = plugin.getDataFolder().getAbsolutePath().replace("\\", "/");
        this.url = "jdbc:h2:file:" + path + "/data;DB_CLOSE_DELAY=-1;AUTO_SERVER=FALSE;TRACE_LEVEL_FILE=0";
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found", e);
        }
        initTables();
        plugin.getLogger().info("[Bob] H2 database initialized.");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private void initTables() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS player_cache (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    ip   VARCHAR(45)
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS bans (
                    uuid      VARCHAR(36) PRIMARY KEY,
                    name      VARCHAR(64),
                    type      VARCHAR(16),
                    reason    VARCHAR(512),
                    banned_by VARCHAR(64),
                    expiry    BIGINT NOT NULL DEFAULT -1
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS mutes (
                    uuid     VARCHAR(36) PRIMARY KEY,
                    name     VARCHAR(64),
                    type     VARCHAR(16),
                    reason   VARCHAR(512),
                    muted_by VARCHAR(64),
                    expiry   BIGINT NOT NULL DEFAULT -1
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS ip_bans (
                    ip        VARCHAR(45) PRIMARY KEY,
                    type      VARCHAR(16),
                    reason    VARCHAR(512),
                    banned_by VARCHAR(64),
                    expiry    BIGINT NOT NULL DEFAULT -1
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS fly_data (
                    uuid            VARCHAR(36) PRIMARY KEY,
                    time_seconds    BIGINT  NOT NULL DEFAULT 0,
                    permanent       BOOLEAN NOT NULL DEFAULT FALSE,
                    last_login_date VARCHAR(16) NOT NULL DEFAULT '',
                    first_join      BOOLEAN NOT NULL DEFAULT FALSE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS daily_reward (
                    uuid          VARCHAR(36) PRIMARY KEY,
                    next_claim_at BIGINT NOT NULL DEFAULT 0
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS warns (
                    uuid     VARCHAR(36) PRIMARY KEY,
                    warns    INT NOT NULL DEFAULT 0,
                    offenses INT NOT NULL DEFAULT 0
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS punishment_history (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    uuid        VARCHAR(36) NOT NULL,
                    target_name VARCHAR(64),
                    type        VARCHAR(16),
                    reason      VARCHAR(512),
                    punisher    VARCHAR(64),
                    duration    VARCHAR(64),
                    timestamp   BIGINT
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS unban_history (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    uuid        VARCHAR(36) NOT NULL,
                    target_name VARCHAR(64),
                    unbanned_by VARCHAR(64),
                    timestamp   BIGINT
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS reports (
                    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                    reporter VARCHAR(64) NOT NULL,
                    target   VARCHAR(64) NOT NULL,
                    reason   VARCHAR(512) NOT NULL,
                    time     VARCHAR(32) NOT NULL
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS player_inventory (
                    uuid           VARCHAR(36) PRIMARY KEY,
                    inventory_data CLOB        NOT NULL,
                    edited         BOOLEAN     NOT NULL DEFAULT FALSE
                )""");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 tables: " + e.getMessage(), e);
        }
    }

    public void close() {
        // H2 DB_CLOSE_DELAY=-1 keeps the file open; JVM shutdown closes it automatically
    }
}

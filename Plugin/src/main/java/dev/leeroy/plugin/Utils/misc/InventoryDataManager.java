package dev.leeroy.plugin.Utils.misc;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.Base64;
import java.util.UUID;

public class InventoryDataManager {

    // Slot indices: 0-35 main, 36 helmet, 37 chest, 38 legs, 39 boots, 40 offhand
    public static final int SLOT_COUNT = 41;

    private final JavaPlugin plugin;
    private final DatabaseManager db;

    public InventoryDataManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    /** Async save. edited=true means an admin modified this while the player was offline. */
    public void save(UUID uuid, ItemStack[] slots, boolean edited) {
        String data = serialize(slots);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveSync(uuid, data, edited));
    }

    /** Convenience overload that extracts slots from a live PlayerInventory. */
    public void save(UUID uuid, PlayerInventory inv, boolean edited) {
        save(uuid, extractFromInventory(inv), edited);
    }

    /** Blocking load — call from an async thread only. Returns null if no row exists. */
    public ItemStack[] load(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT inventory_data FROM player_inventory WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return deserialize(rs.getString("inventory_data"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[InventoryDataManager] load failed: " + e.getMessage());
            return null;
        }
    }

    /** Blocking check — call from an async thread only. */
    public boolean isEdited(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT edited FROM player_inventory WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("edited");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[InventoryDataManager] isEdited failed: " + e.getMessage());
            return false;
        }
    }

    /** Async delete. */
    public void delete(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> deleteSync(uuid));
    }

    /** Blocking delete — call from an async thread only. */
    public void deleteSync(UUID uuid) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM player_inventory WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[InventoryDataManager] delete failed: " + e.getMessage());
        }
    }

    /** Applies slots to a live PlayerInventory. Must be called on the main thread. */
    public static void applyToInventory(PlayerInventory inv, ItemStack[] slots) {
        for (int i = 0; i < 36; i++) inv.setItem(i, slots[i]);
        inv.setHelmet(slots[36]);
        inv.setChestplate(slots[37]);
        inv.setLeggings(slots[38]);
        inv.setBoots(slots[39]);
        inv.setItemInOffHand(slots[40] != null ? slots[40] : new ItemStack(Material.AIR));
    }

    public static ItemStack[] extractFromInventory(PlayerInventory inv) {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < 36; i++) slots[i] = inv.getItem(i);
        slots[36] = inv.getHelmet();
        slots[37] = inv.getChestplate();
        slots[38] = inv.getLeggings();
        slots[39] = inv.getBoots();
        slots[40] = inv.getItemInOffHand();
        return slots;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private static String serialize(ItemStack[] slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i > 0) sb.append('|');
            ItemStack item = i < slots.length ? slots[i] : null;
            if (item != null && !item.getType().isAir()) {
                sb.append(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }
        return sb.toString();
    }

    static ItemStack[] deserialize(String data) {
        String[] parts = data.split("\\|", -1);
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < Math.min(parts.length, SLOT_COUNT); i++) {
            if (!parts[i].isEmpty()) {
                try {
                    slots[i] = ItemStack.deserializeBytes(Base64.getDecoder().decode(parts[i]));
                } catch (Exception e) {
                    slots[i] = null;
                }
            }
        }
        return slots;
    }

    private void saveSync(UUID uuid, String data, boolean edited) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "MERGE INTO player_inventory (uuid, inventory_data, edited) KEY (uuid) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, data);
            ps.setBoolean(3, edited);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[InventoryDataManager] save failed: " + e.getMessage());
        }
    }
}

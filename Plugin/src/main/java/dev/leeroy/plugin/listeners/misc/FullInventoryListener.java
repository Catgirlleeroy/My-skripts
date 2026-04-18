package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FullInventoryListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public FullInventoryListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("full-inventory-warning.enabled", true)) return;

        Player player = event.getPlayer();

        if (player.getInventory().firstEmpty() != -1) return;

        int cooldownSecs = plugin.getConfig().getInt("full-inventory-warning.cooldown", 3);
        long now  = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownSecs * 1000L) return;
        cooldowns.put(player.getUniqueId(), now);

        String raw = plugin.getConfig().getString("full-inventory-warning.message",
                "&c&lWarning! &7Your inventory is full!");
        player.sendActionBar(TextUtil.parse(raw));

        String soundName = plugin.getConfig().getString("full-inventory-warning.sound", "BLOCK_NOTE_BLOCK_PLING");
        if (!soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 2.0f);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}

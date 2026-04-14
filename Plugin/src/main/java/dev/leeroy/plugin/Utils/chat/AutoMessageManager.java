package dev.leeroy.plugin.Utils.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class AutoMessageManager {

    private final JavaPlugin plugin;
    private BukkitTask task;
    private int currentIndex = 0;

    public AutoMessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("auto-messages.enabled", true)) return;

        List<String> messages = config.getStringList("auto-messages.messages");

        if (messages.isEmpty()) return;

        int cooldown = config.getInt("auto-messages.cooldown", 300);
        long ticks = cooldown * 20L;

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            FileConfiguration cfg = plugin.getConfig();
            List<String> msgs = cfg.getStringList("auto-messages.messages");
            if (msgs.isEmpty()) return;

            // Pick a random non-empty message
            List<String> valid = msgs.stream()
                    .filter(m -> !m.trim().isEmpty())
                    .toList();
            if (valid.isEmpty()) return;

            String msg = valid.get(new Random().nextInt(valid.size()));
            String prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("auto-messages.prefix", ""));
            String formatted = ChatColor.translateAlternateColorCodes('&', msg);

            // Broadcast with border
            if (!prefix.isEmpty()) Bukkit.broadcastMessage(prefix);
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(formatted);
            Bukkit.broadcastMessage("");
            if (!prefix.isEmpty()) Bukkit.broadcastMessage(prefix);

            // Play sound
            String soundName = cfg.getString("auto-messages.sound", "");
            if (!soundName.isEmpty()) {
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    Bukkit.getOnlinePlayers().forEach(p ->
                            p.playSound(p.getLocation(), sound, 1.0f, 1.0f)
                    );
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("[Bob] Invalid auto-message sound: " + soundName);
                }
            }

        }, ticks, ticks);
    }

    /** Stops and restarts the task — call this after /bobreload */
    public void restart() {
        if (task != null) task.cancel();
        start();
    }
}
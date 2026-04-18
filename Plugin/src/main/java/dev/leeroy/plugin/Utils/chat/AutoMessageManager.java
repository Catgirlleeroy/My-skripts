package dev.leeroy.plugin.Utils.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class AutoMessageManager {

    private final JavaPlugin plugin;
    private BukkitTask task;

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

            List<String> valid = msgs.stream()
                    .filter(m -> !m.trim().isEmpty())
                    .toList();
            if (valid.isEmpty()) return;

            String msg    = valid.get(new Random().nextInt(valid.size()));
            String prefix = cfg.getString("auto-messages.prefix", "");

            if (!prefix.isEmpty()) Bukkit.broadcast(TextUtil.parse(prefix));
            Bukkit.broadcast(TextUtil.parse(""));
            Bukkit.broadcast(TextUtil.parse(msg));
            Bukkit.broadcast(TextUtil.parse(""));
            if (!prefix.isEmpty()) Bukkit.broadcast(TextUtil.parse(prefix));

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

    public void restart() {
        if (task != null) task.cancel();
        start();
    }
}

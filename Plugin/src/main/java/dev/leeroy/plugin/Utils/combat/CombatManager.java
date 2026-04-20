package dev.leeroy.plugin.Utils.combat;

import dev.leeroy.plugin.Utils.misc.MessagesConfig;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final JavaPlugin plugin;
    private final MessagesConfig messagesConfig;
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();

    public CombatManager(JavaPlugin plugin, MessagesConfig messagesConfig) {
        this.plugin         = plugin;
        this.messagesConfig = messagesConfig;
    }

    public void tag(Player player) {
        if (!plugin.getConfig().getBoolean("combat-tag.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        int duration = plugin.getConfig().getInt("combat-tag.duration", 20);

        BukkitTask existing = combatTasks.remove(uuid);
        if (existing != null) existing.cancel();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            combatTasks.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                online.sendMessage(msg("untagged"));
            }
        }, duration * 20L);

        combatTasks.put(uuid, task);
    }

    public void untag(Player player) {
        BukkitTask task = combatTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public boolean isTagged(UUID uuid) {
        return combatTasks.containsKey(uuid);
    }

    public Component msg(String key) {
        String prefix = messagesConfig.get().getString("combat-tag.prefix", "&8[&c&lCombatTag&8] &7");
        String raw    = messagesConfig.get().getString("combat-tag.messages." + key, "&cMissing: " + key);
        return TextUtil.parse(prefix + raw);
    }
}

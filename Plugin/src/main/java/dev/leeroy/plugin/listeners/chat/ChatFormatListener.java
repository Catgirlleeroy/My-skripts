package dev.leeroy.plugin.listeners.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.misc.BobHooks;
import dev.leeroy.plugin.listeners.misc.VaultHook;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatFormatListener implements Listener {

    private final JavaPlugin plugin;

    public ChatFormatListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-format.enabled", true)) return;

        Player player = event.getPlayer();

        String prefix    = "";
        String groupName = "";

        if (BobHooks.hasVault()) {
            prefix    = VaultHook.getPrefix(player);
            groupName = VaultHook.getGroup(player);
        }

        String format = plugin.getConfig().getString(
                "chat-format.format",
                "{prefix}&f {player}&7: &f{message}"
        );

        format = format
                .replace("{prefix}",  prefix)
                .replace("{group}",   groupName)
                .replace("{name}",    player.getName())
                .replace("{player}",  player.getDisplayName());

        // Split at {message} so the message component is appended cleanly
        String[] parts  = format.split("\\{message\\}", 2);
        String before   = parts[0];
        String after    = parts.length > 1 ? parts[1] : "";

        event.renderer((source, displayName, message, viewer) -> {
            Component result = TextUtil.parse(before).append(message);
            if (!after.isEmpty()) result = result.append(TextUtil.parse(after));
            return result;
        });
    }
}

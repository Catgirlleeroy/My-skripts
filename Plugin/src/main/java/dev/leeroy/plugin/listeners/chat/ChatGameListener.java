package dev.leeroy.plugin.listeners.chat;

import dev.leeroy.plugin.Utils.chat.ChatGameManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatGameListener implements Listener {

    private final ChatGameManager chatGameManager;

    public ChatGameListener(ChatGameManager chatGameManager) {
        this.chatGameManager = chatGameManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!chatGameManager.isActive()) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (chatGameManager.isCorrectAnswer(message)) {
            event.setCancelled(true);
            org.bukkit.Bukkit.getScheduler().runTask(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("Bob"),
                    () -> chatGameManager.handleWin(event.getPlayer(), message)
            );
        }
    }
}

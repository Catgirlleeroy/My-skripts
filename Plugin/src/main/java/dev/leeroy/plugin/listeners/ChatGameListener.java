package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.ChatGameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatGameListener implements Listener {

    private final ChatGameManager chatGameManager;

    public ChatGameListener(ChatGameManager chatGameManager) {
        this.chatGameManager = chatGameManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!chatGameManager.isActive()) return;

        // Run on main thread since dispatchCommand needs it
        org.bukkit.Bukkit.getScheduler().runTask(
                org.bukkit.Bukkit.getPluginManager().getPlugin("Bob"),
                () -> {
                    if (chatGameManager.handleChat(event.getPlayer(), event.getMessage())) {
                        event.setCancelled(true);
                    }
                }
        );
    }
}
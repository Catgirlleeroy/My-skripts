package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;

public class MuteListener implements Listener {

    private final MuteManager muteManager;
    private boolean chatMuted = false;

    public MuteListener(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    public boolean toggleChatMute() {
        chatMuted = !chatMuted;
        return chatMuted;
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        // Server-wide chat mute — bypass for players with permission
        if (chatMuted && !event.getPlayer().hasPermission("bob.chatmute.bypass")) {
            event.getPlayer().sendMessage(ChatColor.RED + "Chat is currently muted.");
            event.setCancelled(true);
            return;
        }

        // Individual mute check
        if (muteManager.isMuted(event.getPlayer().getUniqueId())) {
            Map<String, Object> details = muteManager.getMuteDetails(event.getPlayer().getUniqueId());
            if (details != null) {
                String type   = (String) details.get("type");
                String reason = (String) details.get("reason");
                long   expiry = (long)   details.get("expiry");

                StringBuilder msg = new StringBuilder();
                msg.append(ChatColor.RED).append("You are muted. Reason: ").append(reason);
                if (type.equals("temp") && expiry != -1L) {
                    msg.append(ChatColor.RED).append(" | Expires in: ")
                            .append(ChatColor.WHITE).append(BanManager.formatRemaining(expiry));
                }
                event.getPlayer().sendMessage(msg.toString());
            }
            event.setCancelled(true);
        }
    }
}
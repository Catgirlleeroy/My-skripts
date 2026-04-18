package dev.leeroy.plugin.listeners.punishment;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.MuteManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
    public void onChat(AsyncChatEvent event) {
        // Server-wide chat mute — bypass for players with permission
        if (chatMuted && !event.getPlayer().hasPermission("bob.chatmute.bypass")) {
            event.getPlayer().sendMessage(Component.text("Chat is currently muted.", NamedTextColor.RED));
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
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

                StringBuilder msg = new StringBuilder("&cYou are muted. Reason: " + reason);
                if (type.equals("temp") && expiry != -1L) {
                    msg.append(" &c| Expires in: &f").append(BanManager.formatRemaining(expiry));
                }
                event.getPlayer().sendMessage(TextUtil.parse(msg.toString()));
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
            }
            event.setCancelled(true);
        }
    }
}

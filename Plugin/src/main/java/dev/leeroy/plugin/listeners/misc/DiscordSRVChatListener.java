package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.chat.ChatGameManager;
import dev.leeroy.plugin.Utils.punishment.MuteManager;
import dev.leeroy.plugin.listeners.punishment.MuteListener;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import org.bukkit.entity.Player;

public class DiscordSRVChatListener {

    private final MuteManager muteManager;
    private final MuteListener muteListener;
    private final ChatGameManager chatGameManager;

    public DiscordSRVChatListener(MuteManager muteManager, MuteListener muteListener, ChatGameManager chatGameManager) {
        this.muteManager     = muteManager;
        this.muteListener    = muteListener;
        this.chatGameManager = chatGameManager;
    }

    @Subscribe
    public void onDiscordChat(GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();
        if (muteListener.isChatMuted() && !player.hasPermission("bob.chatmute.bypass")) {
            event.setCancelled(true);
            return;
        }
        if (muteManager.isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        // Suppress chat game answers — the winning message should never appear in Discord
        if (chatGameManager.isActive() && chatGameManager.isCorrectAnswer(event.getMessage())) {
            event.setCancelled(true);
        }
    }
}

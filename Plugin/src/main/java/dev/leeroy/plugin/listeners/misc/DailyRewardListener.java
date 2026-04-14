package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.misc.DailyRewardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class DailyRewardListener implements Listener {

    private final DailyRewardManager dailyManager;

    public DailyRewardListener(DailyRewardManager dailyManager) {
        this.dailyManager = dailyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Initialize player if they've never joined before
        dailyManager.initPlayer(event.getPlayer().getUniqueId());
    }
}
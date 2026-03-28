package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.PlayerCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerCacheListener implements Listener {

    private final PlayerCache playerCache;

    public PlayerCacheListener(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        playerCache.store(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        playerCache.storeIP(event.getPlayer().getUniqueId(), event.getAddress().getHostAddress());
    }
}
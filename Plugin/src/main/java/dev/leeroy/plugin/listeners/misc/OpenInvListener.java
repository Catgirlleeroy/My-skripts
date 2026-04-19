package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.commands.misc.OpenInvCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class OpenInvListener implements Listener {

    private final JavaPlugin plugin;

    public OpenInvListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMirrorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID targetId = OpenInvCommand.openViews.get(viewer.getUniqueId());
        if (targetId == null) return;

        int raw = event.getRawSlot();

        // Block the unused trailing slots
        if (raw >= 41 && raw <= 44) {
            event.setCancelled(true);
            return;
        }

        // Schedule a 1-tick delayed sync so the click resolves first
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) return;

        Inventory top = event.getView().getTopInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline() && target.isOnline()) {
                OpenInvCommand.syncBack(top, target);
            }
        }, 1L);
    }

    @EventHandler
    public void onViewerClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID targetId = OpenInvCommand.openViews.remove(viewer.getUniqueId());
        if (targetId == null) return;

        Player target = Bukkit.getPlayer(targetId);
        if (target != null && target.isOnline()) {
            OpenInvCommand.syncBack(event.getInventory(), target);
        }
    }

    @EventHandler
    public void onTargetQuit(PlayerQuitEvent event) {
        UUID targetId = event.getPlayer().getUniqueId();
        OpenInvCommand.openViews.entrySet().removeIf(entry -> {
            if (!entry.getValue().equals(targetId)) return false;
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null) {
                OpenInvCommand.syncBack(viewer.getOpenInventory().getTopInventory(), event.getPlayer());
                viewer.closeInventory();
                viewer.sendMessage(Component.text(event.getPlayer().getName() + " left — inventory closed.", NamedTextColor.GRAY));
            }
            return true;
        });
    }
}

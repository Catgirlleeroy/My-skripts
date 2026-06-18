package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.Utils.misc.InventoryDataManager;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.commands.misc.OpenInvCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class OpenInvListener implements Listener {

    private final JavaPlugin plugin;
    private final InventoryDataManager invData;

    public OpenInvListener(JavaPlugin plugin, InventoryDataManager invData) {
        this.plugin = plugin;
        this.invData = invData;
    }

    @EventHandler
    public void onMirrorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();

        boolean isOnline  = OpenInvCommand.openViews.containsKey(viewerId);
        boolean isOffline = OpenInvCommand.offlineViews.containsKey(viewerId);
        if (!isOnline && !isOffline) return;

        int raw = event.getRawSlot();

        // Block the read-only main-hand slot and unused trailing slots
        if (raw >= 41 && raw <= 44) {
            event.setCancelled(true);
            return;
        }

        // Block taking placeholder glass panes out of empty armor/offhand slots
        if (raw >= 36 && raw <= 40 && OpenInvCommand.isPlaceholder(event.getCurrentItem())) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                event.setCancelled(true);
                return;
            }
        }

        if (isOnline) {
            UUID targetId = OpenInvCommand.openViews.get(viewerId);
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) return;

            Inventory top = event.getView().getTopInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (viewer.isOnline() && target.isOnline()) {
                    OpenInvCommand.syncBack(top, target);
                }
            }, 1L);
        }
        // Offline views: item movement is handled by Minecraft in the GUI — saved on close
    }

    @EventHandler
    public void onViewerClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();

        // Online target close
        UUID targetId = OpenInvCommand.openViews.remove(viewerId);
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                OpenInvCommand.syncBack(event.getInventory(), target);
            }
            return;
        }

        // Offline target close — save mirror contents to H2 as edited
        UUID offlineTargetId = OpenInvCommand.offlineViews.remove(viewerId);
        if (offlineTargetId != null) {
            ItemStack[] slots = OpenInvCommand.extractFromMirror(event.getInventory());
            invData.save(offlineTargetId, slots, true);
            viewer.sendMessage(TextUtil.prefixed(
                    Component.text("Offline inventory saved.", NamedTextColor.GREEN)));
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

    /** Save a snapshot of every player's inventory on quit so admins can view/edit it offline. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        invData.save(player.getUniqueId(), player.getInventory(), false);
    }

    /** On join, apply any pending admin edit and notify the player. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!invData.isEdited(uuid)) {
                // No pending admin edit — still clean up the snapshot row
                invData.deleteSync(uuid);
                return;
            }
            ItemStack[] slots = invData.load(uuid);
            invData.deleteSync(uuid);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || slots == null) return;
                InventoryDataManager.applyToInventory(player.getInventory(), slots);
                player.sendMessage(TextUtil.prefixed(
                        Component.text("Your inventory was updated by an admin while you were offline.", NamedTextColor.YELLOW)));
            });
        });
    }
}

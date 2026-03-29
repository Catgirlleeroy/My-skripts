package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PunishConfig;
import dev.leeroy.plugin.gui.PunishGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PunishListener implements Listener {

    private final JavaPlugin plugin;
    private final BanManager banManager;
    private final IPBanManager ipBanManager;
    private final MuteManager muteManager;
    private final PunishConfig punishConfig;

    private final Map<UUID, String> guiState = new HashMap<>();

    public PunishListener(JavaPlugin plugin, BanManager banManager,
                          IPBanManager ipBanManager, MuteManager muteManager,
                          PunishConfig punishConfig) {
        this.plugin       = plugin;
        this.banManager   = banManager;
        this.ipBanManager = ipBanManager;
        this.muteManager  = muteManager;
        this.punishConfig = punishConfig;
    }

    // ── Sneak + hit ───────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player staff)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!staff.hasPermission("bob.punish")) return;
        if (!staff.isSneaking()) return;

        event.setCancelled(true);
        PunishGUI.openActionGUI(staff, target, punishConfig.get());
    }

    // ── Inventory click ───────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!title.contains("Punish") && !title.contains("Reason:") && !title.contains("Duration:")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType().isAir()) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        if (title.contains("Reason:")) {
            handleReasonClick(staff, itemName);
        } else if (title.contains("Duration:")) {
            handleDurationClick(staff, itemName);
        } else if (title.contains("Punish")) {
            handleActionClick(staff, title, event.getCurrentItem(), itemName);
        }
    }

    // ── Stage 1: Action ───────────────────────────────────────────────────────

    private void handleActionClick(Player staff, String title, org.bukkit.inventory.ItemStack clicked, String itemName) {
        String closeName = ChatColor.stripColor(PunishGUI.color(
                punishConfig.get().getString("close-button.name", "&7Close")));
        if (itemName.equals(closeName)) { staff.closeInventory(); return; }

        // Check no-permission lore
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (ChatColor.stripColor(line).contains("You do not have permission")) {
                    staff.sendMessage(ChatColor.RED + "You don't have permission to perform that action.");
                    return;
                }
            }
        }

        // Extract target name from title
        String targetName = title
                .replaceAll(".*Punish\\s+", "")
                .replaceAll("\\s*[«»].*", "")
                .trim();

        Player target = Bukkit.getPlayerExact(targetName);

        // Match item name to action key by comparing config names
        String action = resolveActionFromName(itemName);

        if (action == null) return;

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "Player '" + targetName + "' is no longer online.");
            staff.closeInventory();
            return;
        }

        guiState.put(staff.getUniqueId(), action + ":" + targetName);
        PunishGUI.openReasonGUI(staff, targetName, action, punishConfig.get());
    }

    // ── Stage 2: Reason ───────────────────────────────────────────────────────

    private void handleReasonClick(Player staff, String reason) {
        String state = guiState.get(staff.getUniqueId());
        if (state == null) { staff.closeInventory(); return; }

        String[] parts = state.split(":", 2);
        String action     = parts[0];
        String targetName = parts[1];

        if (action.equals("tempban") || action.equals("tempmute") || action.equals("tempipban")) {
            guiState.put(staff.getUniqueId(), action + ":" + targetName + ":" + reason);
            PunishGUI.openDurationGUI(staff, targetName, action, reason, punishConfig.get());
        } else {
            executePunishment(staff, targetName, action, reason, null);
            guiState.remove(staff.getUniqueId());
        }
    }

    // ── Stage 3: Duration ─────────────────────────────────────────────────────

    private void handleDurationClick(Player staff, String duration) {
        String state = guiState.get(staff.getUniqueId());
        if (state == null) { staff.closeInventory(); return; }

        String[] parts    = state.split(":", 3);
        String action     = parts[0];
        String targetName = parts[1];
        String reason     = parts[2];

        executePunishment(staff, targetName, action, reason, duration);
        guiState.remove(staff.getUniqueId());
    }

    // ── Execute ───────────────────────────────────────────────────────────────

    private void executePunishment(Player staff, String targetName, String action,
                                   String reason, String duration) {
        Player target = Bukkit.getPlayerExact(targetName);
        YamlConfiguration cfg = punishConfig.get();
        staff.closeInventory();

        String broadcastPath = "actions." + action + ".messages.broadcast";
        String kickPath      = "actions." + action + ".messages.kick";
        String selfPath      = "actions." + action + ".messages.notify-self";

        switch (action) {
            case "ban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                if (banManager.isBanned(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already banned."); return; }
                banManager.ban(target.getUniqueId(), target.getName(), reason, staff.getName());
                strikeFX(target);
                String kickMsg = PunishGUI.formatMessage(cfg, kickPath, targetName, staff.getName(), reason, null);
                Bukkit.getScheduler().runTaskLater(plugin, () -> target.kickPlayer(kickMsg), 10L);
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, null));
            }
            case "tempban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                long ms = BanManager.parseDuration(duration);
                if (ms == -1) { staff.sendMessage(ChatColor.RED + "Invalid duration."); return; }
                if (banManager.isBanned(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already banned."); return; }
                banManager.tempBan(target.getUniqueId(), target.getName(), reason, staff.getName(), ms);
                strikeFX(target);
                String remaining = BanManager.formatRemaining(System.currentTimeMillis() + ms);
                String kickMsg = PunishGUI.formatMessage(cfg, kickPath, targetName, staff.getName(), reason, remaining);
                Bukkit.getScheduler().runTaskLater(plugin, () -> target.kickPlayer(kickMsg), 10L);
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, duration));
            }
            case "mute" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                if (muteManager.isMuted(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already muted."); return; }
                muteManager.mute(target.getUniqueId(), target.getName(), reason, staff.getName());
                target.sendMessage(PunishGUI.formatMessage(cfg, selfPath, targetName, staff.getName(), reason, null));
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, null));
            }
            case "tempmute" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                long ms = BanManager.parseDuration(duration);
                if (ms == -1) { staff.sendMessage(ChatColor.RED + "Invalid duration."); return; }
                if (muteManager.isMuted(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already muted."); return; }
                muteManager.tempMute(target.getUniqueId(), target.getName(), reason, staff.getName(), ms);
                target.sendMessage(PunishGUI.formatMessage(cfg, selfPath, targetName, staff.getName(), reason, duration));
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, duration));
            }
            case "kick" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                String kickMsg = PunishGUI.formatMessage(cfg, kickPath, targetName, staff.getName(), reason, null);
                target.kickPlayer(kickMsg);
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, null));
            }
            case "ipban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                String ip = target.getAddress().getAddress().getHostAddress();
                if (ipBanManager.isBanned(ip)) { staff.sendMessage(ChatColor.RED + targetName + "'s IP is already banned."); return; }
                ipBanManager.ban(ip, reason, staff.getName());
                strikeFX(target);
                String kickMsg = PunishGUI.formatMessage(cfg, kickPath, targetName, staff.getName(), reason, null);
                Bukkit.getScheduler().runTaskLater(plugin, () -> target.kickPlayer(kickMsg), 10L);
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, null));
            }
            case "tempipban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                long ms = BanManager.parseDuration(duration);
                if (ms == -1) { staff.sendMessage(ChatColor.RED + "Invalid duration."); return; }
                String ip = target.getAddress().getAddress().getHostAddress();
                if (ipBanManager.isBanned(ip)) { staff.sendMessage(ChatColor.RED + targetName + "'s IP is already banned."); return; }
                ipBanManager.tempBan(ip, reason, staff.getName(), ms);
                strikeFX(target);
                String remaining = BanManager.formatRemaining(System.currentTimeMillis() + ms);
                String kickMsg = PunishGUI.formatMessage(cfg, kickPath, targetName, staff.getName(), reason, remaining);
                Bukkit.getScheduler().runTaskLater(plugin, () -> target.kickPlayer(kickMsg), 10L);
                Bukkit.broadcastMessage(PunishGUI.formatMessage(cfg, broadcastPath, targetName, staff.getName(), reason, duration));
            }
        }

        staff.sendMessage(ChatColor.GREEN + "✦ Successfully applied " + action + " to " + targetName + ".");
        staff.playSound(staff.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Matches a clicked item's display name back to its action key
     * by comparing against config names.
     */
    private String resolveActionFromName(String strippedName) {
        YamlConfiguration cfg = punishConfig.get();
        for (String action : List.of("ban", "tempban", "mute", "tempmute", "kick", "ipban", "tempipban")) {
            String configName = ChatColor.stripColor(
                    PunishGUI.color(cfg.getString("actions." + action + ".name", "")));
            if (configName.equalsIgnoreCase(strippedName)) return action;
        }
        return null;
    }

    private void strikeFX(Player target) {
        target.getWorld().strikeLightningEffect(target.getLocation());
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
        );
    }
}
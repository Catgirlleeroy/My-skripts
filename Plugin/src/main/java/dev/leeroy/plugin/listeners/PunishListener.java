package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.MuteManager;
import dev.leeroy.plugin.Utils.PunishConfig;
import dev.leeroy.plugin.gui.PunishGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PunishListener implements Listener {

    private final JavaPlugin plugin;
    private final BanManager banManager;
    private final IPBanManager ipBanManager;
    private final MuteManager muteManager;
    private final PunishConfig punishConfig;

    // stage 1: "action:targetName"
    // stage 2: "action:targetName:reason"
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

    // ── Sneak + hit to open GUI ───────────────────────────────────────────────

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player staff)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!staff.hasPermission("bob.punish")) return;
        if (!staff.isSneaking()) return;

        event.setCancelled(true);
        PunishGUI.openActionGUI(staff, target, punishConfig.get());
    }

    // ── Inventory click handler ───────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        // Strip color from raw title for matching
        String rawTitle = event.getView().getTitle();
        String title = ChatColor.stripColor(rawTitle);

        // Only handle our GUIs
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
            handleActionClick(staff, title, itemName);
        }
    }

    // ── Stage 1: Action ───────────────────────────────────────────────────────

    private void handleActionClick(Player staff, String title, String itemName) {
        if (itemName.equals("Close")) { staff.closeInventory(); return; }

        // Extract target name — title format is "» Punish PlayerName «" after strip
        // Remove leading/trailing symbols and "Punish" keyword
        String targetName = title
                .replaceAll(".*Punish\\s+", "")  // remove everything up to and including "Punish "
                .replaceAll("\\s*[«»].*", "")     // remove trailing « and anything after
                .trim();

        Player target = Bukkit.getPlayerExact(targetName);

        String action = switch (itemName) {
            case "Ban"      -> "ban";
            case "Tempban"  -> "tempban";
            case "Mute"     -> "mute";
            case "Tempmute" -> "tempmute";
            case "Kick"     -> "kick";
            case "IP Ban"   -> "ipban";
            default         -> null;
        };

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

        if (action.equals("tempban") || action.equals("tempmute")) {
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

        String[] parts = state.split(":", 3);
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
        staff.closeInventory();

        switch (action) {
            case "ban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                if (banManager.isBanned(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already banned."); return; }
                banManager.ban(target.getUniqueId(), target.getName(), reason, staff.getName());
                strikeFX(target);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        target.kickPlayer(ChatColor.RED + "You have been permanently banned.\n" + ChatColor.WHITE + "Reason: " + reason), 10L);
                Bukkit.broadcastMessage(ChatColor.RED + "[BAN] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been permanently banned! " + ChatColor.GRAY + "Reason: " + reason);
            }
            case "tempban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                long ms = BanManager.parseDuration(duration);
                if (ms == -1) { staff.sendMessage(ChatColor.RED + "Invalid duration."); return; }
                if (banManager.isBanned(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already banned."); return; }
                banManager.tempBan(target.getUniqueId(), target.getName(), reason, staff.getName(), ms);
                strikeFX(target);
                final long fMs = ms;
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        target.kickPlayer(ChatColor.RED + "You have been temporarily banned.\n"
                                + ChatColor.WHITE + "Duration: " + BanManager.formatRemaining(System.currentTimeMillis() + fMs) + "\n"
                                + ChatColor.WHITE + "Reason: " + reason), 10L);
                Bukkit.broadcastMessage(ChatColor.RED + "[TEMPBAN] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been temporarily banned! " + ChatColor.GRAY + "Duration: " + duration + " | Reason: " + reason);
            }
            case "mute" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                if (muteManager.isMuted(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already muted."); return; }
                muteManager.mute(target.getUniqueId(), target.getName(), reason, staff.getName());
                target.sendMessage(ChatColor.RED + "You have been permanently muted. Reason: " + reason);
                Bukkit.broadcastMessage(ChatColor.RED + "[MUTE] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been muted! " + ChatColor.GRAY + "Reason: " + reason);
            }
            case "tempmute" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                long ms = BanManager.parseDuration(duration);
                if (ms == -1) { staff.sendMessage(ChatColor.RED + "Invalid duration."); return; }
                if (muteManager.isMuted(target.getUniqueId())) { staff.sendMessage(ChatColor.RED + targetName + " is already muted."); return; }
                muteManager.tempMute(target.getUniqueId(), target.getName(), reason, staff.getName(), ms);
                target.sendMessage(ChatColor.RED + "You have been muted for " + duration + ". Reason: " + reason);
                Bukkit.broadcastMessage(ChatColor.RED + "[TEMPMUTE] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been muted! " + ChatColor.GRAY + "Duration: " + duration + " | Reason: " + reason);
            }
            case "kick" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                target.kickPlayer(ChatColor.RED + "You have been kicked.\n" + ChatColor.WHITE + "Reason: " + reason);
                Bukkit.broadcastMessage(ChatColor.RED + "[KICK] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been kicked! " + ChatColor.GRAY + "Reason: " + reason);
            }
            case "ipban" -> {
                if (target == null) { staff.sendMessage(ChatColor.RED + targetName + " is no longer online."); return; }
                String ip = target.getAddress().getAddress().getHostAddress();
                if (ipBanManager.isBanned(ip)) { staff.sendMessage(ChatColor.RED + targetName + "'s IP is already banned."); return; }
                ipBanManager.ban(ip, reason, staff.getName());
                strikeFX(target);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        target.kickPlayer(ChatColor.RED + "You have been permanently IP banned.\n" + ChatColor.WHITE + "Reason: " + reason), 10L);
                Bukkit.broadcastMessage(ChatColor.RED + "[IP-BAN] " + ChatColor.YELLOW + targetName + ChatColor.RED + " has been permanently IP banned! " + ChatColor.GRAY + "Reason: " + reason);
            }
        }

        staff.sendMessage(ChatColor.GREEN + "✦ Successfully applied " + action + " to " + targetName + ".");
        staff.playSound(staff.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void strikeFX(Player target) {
        target.getWorld().strikeLightning(target.getLocation());
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
        );
    }

    private boolean isActionTitle(String title) {
        return title.contains("» Punish") && !title.contains("» Reason") && !title.contains("» Duration");
    }
}
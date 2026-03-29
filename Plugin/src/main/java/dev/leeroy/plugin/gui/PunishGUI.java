package dev.leeroy.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PunishGUI {

    public static final String ACTION_TITLE_PREFIX   = "» Punish";
    public static final String REASON_TITLE_PREFIX   = "» Reason: ";
    public static final String DURATION_TITLE_PREFIX = "» Duration: ";

    // ── Action selection GUI ─────────────────────────────────────────────────

    public static void openActionGUI(Player staff, Player target, YamlConfiguration config) {
        String rawTitle = config.getString("gui-title", "&8» Punish &c{player} &8«");
        String title = color(rawTitle.replace("{player}", target.getName()));

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(target);
        skull.setDisplayName(ChatColor.YELLOW + target.getName());
        skull.setLore(List.of(ChatColor.GRAY + "Select a punishment below."));
        head.setItemMeta(skull);
        inv.setItem(4, head);

        // Action slots
        String[] actions  = {"ban", "tempban", "mute", "tempmute", "kick", "ipban"};
        int[]    slots    = {10, 11, 12, 13, 14, 15};

        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            if (!config.getBoolean("actions." + action + ".enabled", true)) continue;
            inv.setItem(slots[i], makeActionItem(staff, action, target.getName(), config));
        }

        // Close button
        String closeMat  = config.getString("close-button.item", "DARK_OAK_DOOR");
        String closeName = color(config.getString("close-button.name", "&7Close"));
        inv.setItem(26, makeItem(parseMaterial(closeMat, Material.DARK_OAK_DOOR), closeName, List.of()));

        staff.openInventory(inv);
    }

    // ── Reason selection GUI ─────────────────────────────────────────────────

    public static void openReasonGUI(Player staff, String targetName, String action, YamlConfiguration config) {
        String rawTitle = config.getString("reason-gui.title", "&8» Reason: &c{action}");
        String title = color(rawTitle.replace("{action}", action));

        // Use per-action reasons
        List<String> reasons = config.getStringList("actions." + action + ".reasons");
        Material mat = parseMaterial(config.getString("reason-gui.item", "PAPER"), Material.PAPER);

        int size = Math.max(9, (int)(Math.ceil(reasons.size() / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < reasons.size(); i++) {
            inv.setItem(i, makeItem(mat, ChatColor.YELLOW + reasons.get(i),
                    List.of(ChatColor.GRAY + "Click to select this reason.")));
        }

        staff.openInventory(inv);
    }

    public static void openDurationGUI(Player staff, String targetName, String action,
                                       String reason, YamlConfiguration config) {
        String rawTitle = config.getString("duration-gui.title", "&8» Duration: &6{action}");
        String title = color(rawTitle.replace("{action}", action));

        // Use per-action durations
        List<String> durations = config.getStringList("actions." + action + ".durations");
        Material mat = parseMaterial(config.getString("duration-gui.item", "CLOCK"), Material.CLOCK);

        int size = Math.max(9, (int)(Math.ceil(durations.size() / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < durations.size(); i++) {
            inv.setItem(i, makeItem(mat, ChatColor.AQUA + durations.get(i),
                    List.of(ChatColor.GRAY + "Click to select this duration.")));
        }

        staff.openInventory(inv);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack makeActionItem(Player staff, String action,
                                            String targetName, YamlConfiguration config) {
        String path    = "actions." + action + ".";
        String perm    = "bob." + action.replace("ipban", "ipban");
        boolean hasPerm = staff.hasPermission("bob." + action);

        if (!hasPerm) {
            // No permission — show barrier with config lore
            String noPermItem = config.getString("no-permission.item", "BARRIER");
            List<String> rawLore = config.getStringList("no-permission.lore");
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(color(line.replace("{permission}", "bob." + action)));
            }
            String name = color(config.getString(path + "name", "&c" + action));
            return makeItem(parseMaterial(noPermItem, Material.BARRIER), name, lore);
        }

        String matStr  = config.getString(path + "item", "STONE");
        String name    = color(config.getString(path + "name", "&f" + action)
                .replace("{player}", targetName));

        List<String> rawLore = config.getStringList(path + "lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(color(line.replace("{player}", targetName)));
        }

        return makeItem(parseMaterial(matStr, Material.STONE), name, lore);
    }

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    /**
     * Formats a punishment message from config, replacing all placeholders.
     */
    public static String formatMessage(YamlConfiguration config, String path,
                                       String player, String staff,
                                       String reason, String duration) {
        String raw = config.getString(path, "");
        return color(raw
                .replace("{player}",   player)
                .replace("{staff}",    staff != null ? staff : "")
                .replace("{reason}",   reason != null ? reason : "")
                .replace("{duration}", duration != null ? duration : "")
        );
    }
}
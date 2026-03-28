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

import java.util.List;

public class PunishGUI {

    public static final String ACTION_TITLE_PREFIX  = ChatColor.DARK_GRAY + "» Punish: ";
    public static final String REASON_TITLE_PREFIX  = ChatColor.DARK_GRAY + "» Reason: ";
    public static final String DURATION_TITLE_PREFIX = ChatColor.DARK_GRAY + "» Duration: ";

    // ── Action selection GUI ─────────────────────────────────────────────────

    public static void openActionGUI(Player staff, Player target, YamlConfiguration config) {
        String rawTitle = config.getString("gui-title", "&8» Punish &c{player} &8«");
        String title = ChatColor.translateAlternateColorCodes('&',
                rawTitle.replace("{player}", target.getName()));

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Player head in center top
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(ChatColor.YELLOW + target.getName());
        skullMeta.setLore(List.of(ChatColor.GRAY + "Select a punishment below."));
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // Actions — only show if enabled in config
        if (config.getBoolean("actions.ban", true))
            inv.setItem(10, makeItem(Material.BARRIER,        ChatColor.RED           + "Ban",     List.of(ChatColor.GRAY + "Permanently ban " + target.getName())));
        if (config.getBoolean("actions.tempban", true))
            inv.setItem(11, makeItem(Material.IRON_BARS,      ChatColor.GOLD          + "Tempban", List.of(ChatColor.GRAY + "Temporarily ban " + target.getName())));
        if (config.getBoolean("actions.mute", true))
            inv.setItem(12, makeItem(Material.STRING,         ChatColor.YELLOW        + "Mute",    List.of(ChatColor.GRAY + "Permanently mute " + target.getName())));
        if (config.getBoolean("actions.tempmute", true))
            inv.setItem(13, makeItem(Material.LEAD,           ChatColor.GREEN         + "Tempmute",List.of(ChatColor.GRAY + "Temporarily mute " + target.getName())));
        if (config.getBoolean("actions.kick", true))
            inv.setItem(14, makeItem(Material.LEATHER_BOOTS,  ChatColor.AQUA          + "Kick",    List.of(ChatColor.GRAY + "Kick " + target.getName())));
        if (config.getBoolean("actions.ipban", true))
            inv.setItem(15, makeItem(Material.NETHER_BRICK,   ChatColor.DARK_RED      + "IP Ban",  List.of(ChatColor.GRAY + "IP ban " + target.getName())));

        // Close button
        inv.setItem(26, makeItem(Material.DARK_OAK_DOOR, ChatColor.GRAY + "Close", List.of()));

        staff.openInventory(inv);
    }

    // ── Reason selection GUI ─────────────────────────────────────────────────

    public static void openReasonGUI(Player staff, String targetName, String action, YamlConfiguration config) {
        String title = REASON_TITLE_PREFIX + ChatColor.RED + action;
        List<String> reasons = config.getStringList("reasons");

        int size = (int) (Math.ceil((reasons.size() + 1) / 9.0) * 9);
        size = Math.max(size, 9);
        Inventory inv = Bukkit.createInventory(null, size, title);

        Material[] mats = {
                Material.PAPER, Material.BOOK, Material.WRITABLE_BOOK,
                Material.BOOKSHELF, Material.ENCHANTED_BOOK, Material.MAP,
                Material.KNOWLEDGE_BOOK, Material.NAME_TAG, Material.COMPASS
        };

        for (int i = 0; i < reasons.size(); i++) {
            Material mat = mats[i % mats.length];
            inv.setItem(i, makeItem(mat, ChatColor.YELLOW + reasons.get(i),
                    List.of(ChatColor.GRAY + "Click to select this reason.")));
        }

        staff.openInventory(inv);
    }

    // ── Duration selection GUI ───────────────────────────────────────────────

    public static void openDurationGUI(Player staff, String targetName, String action, String reason, YamlConfiguration config) {
        String title = DURATION_TITLE_PREFIX + ChatColor.GOLD + action;
        List<String> durations = config.getStringList("durations");

        int size = (int) (Math.ceil((durations.size() + 1) / 9.0) * 9);
        size = Math.max(size, 9);
        Inventory inv = Bukkit.createInventory(null, size, title);

        Material[] mats = {
                Material.CLOCK, Material.REPEATER, Material.COMPARATOR,
                Material.DAYLIGHT_DETECTOR, Material.OBSERVER, Material.PISTON,
                Material.HOPPER, Material.DROPPER, Material.DISPENSER
        };

        for (int i = 0; i < durations.size(); i++) {
            Material mat = mats[i % mats.length];
            inv.setItem(i, makeItem(mat, ChatColor.AQUA + durations.get(i),
                    List.of(ChatColor.GRAY + "Click to select this duration.")));
        }

        staff.openInventory(inv);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
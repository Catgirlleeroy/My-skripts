package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.misc.InventoryDataManager;
import dev.leeroy.plugin.Utils.misc.PlayerCache;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OpenInvCommand implements BasicCommand {

    /** Marks items that are slot-label placeholders — never written back to the target. */
    public static final NamespacedKey PLACEHOLDER = new NamespacedKey("bob", "openinv_placeholder");

    /** Maps viewer UUID → target UUID for online targets. */
    public static final Map<UUID, UUID> openViews = new ConcurrentHashMap<>();

    /** Maps viewer UUID → target UUID for offline targets. */
    public static final Map<UUID, UUID> offlineViews = new ConcurrentHashMap<>();

    // Mirror slot layout (45 slots / 5 rows):
    //   0–35 → main inventory (PlayerInventory indices 0–35)
    //     36 → Helmet
    //     37 → Chestplate
    //     38 → Leggings
    //     39 → Boots
    //     40 → Off-Hand  (label: OFF)
    //     41 → Main-Hand (label: MAIN, read-only)
    //  42–44 → unused, clicks cancelled

    public static final int SLOT_HELMET   = 36;
    public static final int SLOT_CHEST    = 37;
    public static final int SLOT_LEGS     = 38;
    public static final int SLOT_BOOTS    = 39;
    public static final int SLOT_OFFHAND  = 40;
    public static final int SLOT_MAINHAND = 41;

    private final JavaPlugin plugin;
    private final PlayerCache playerCache;
    private final InventoryDataManager invData;

    public OpenInvCommand(JavaPlugin plugin, PlayerCache playerCache, InventoryDataManager invData) {
        this.plugin = plugin;
        this.playerCache = playerCache;
        this.invData = invData;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Only players can use this command.", NamedTextColor.RED)));
            return;
        }
        if (args.length == 0) {
            player.sendMessage(TextUtil.prefixed(Component.text("Usage: /openinv <player>", NamedTextColor.RED)));
            return;
        }

        // Check if the target is online first
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null && target.isOnline()) {
            if (target.equals(player)) {
                player.sendMessage(TextUtil.prefixed(Component.text("You cannot open your own inventory.", NamedTextColor.RED)));
                return;
            }
            openViews.put(player.getUniqueId(), target.getUniqueId());
            player.openInventory(buildMirror(target));
            player.sendMessage(TextUtil.prefixed(Component.text("Opened inventory of ", NamedTextColor.GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.GRAY))));
            return;
        }

        // Target is offline — look up UUID from cache and load from H2
        UUID targetUUID = playerCache.resolveUUID(args[0]);
        if (targetUUID == null) {
            player.sendMessage(TextUtil.prefixed(Component.text("Player not found.", NamedTextColor.RED)));
            return;
        }
        String targetName = playerCache.getName(targetUUID);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] slots = invData.load(targetUUID);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (slots == null) {
                    player.sendMessage(TextUtil.prefixed(Component.text(
                            targetName + " has no saved inventory data.", NamedTextColor.RED)));
                    return;
                }
                offlineViews.put(player.getUniqueId(), targetUUID);
                player.openInventory(buildMirrorFromSlots(targetName, slots));
                player.sendMessage(TextUtil.prefixed(Component.text("Opened offline inventory of ", NamedTextColor.GRAY)
                        .append(Component.text(targetName, NamedTextColor.YELLOW))
                        .append(Component.text(".", NamedTextColor.GRAY))));
            });
        });
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("bob.openinv");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            Entity executor = stack.getExecutor();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> executor == null || !p.equals(executor))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .toList();
        }
        return List.of();
    }

    // ── Mirror building ──────────────────────────────────────────────────────

    public static Inventory buildMirror(Player target) {
        return buildMirrorFromSlots(target.getName(), InventoryDataManager.extractFromInventory(target.getInventory()));
    }

    public static Inventory buildMirrorFromSlots(String name, ItemStack[] slots) {
        Inventory mirror = Bukkit.createInventory(null, 45,
                Component.text(name + "'s Inventory", NamedTextColor.DARK_GRAY));

        for (int i = 0; i < 36; i++) {
            mirror.setItem(i, slots[i]);
        }

        mirror.setItem(SLOT_HELMET,  armorSlot(slots[36], "Helmet"));
        mirror.setItem(SLOT_CHEST,   armorSlot(slots[37], "Chestplate"));
        mirror.setItem(SLOT_LEGS,    armorSlot(slots[38], "Leggings"));
        mirror.setItem(SLOT_BOOTS,   armorSlot(slots[39], "Boots"));
        mirror.setItem(SLOT_OFFHAND, armorSlot(slots[40], "OFF"));

        return mirror;
    }

    /** Returns the real item if present, otherwise a named placeholder glass. */
    private static ItemStack armorSlot(ItemStack item, String label) {
        if (item != null && item.getType() != Material.AIR) return item;
        return placeholder(label);
    }

    private static ItemStack placeholder(String name) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text("[ " + name + " ]", NamedTextColor.GRAY));
        meta.getPersistentDataContainer().set(PLACEHOLDER, PersistentDataType.BYTE, (byte) 1);
        glass.setItemMeta(meta);
        return glass;
    }

    public static boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(PLACEHOLDER, PersistentDataType.BYTE);
    }

    // ── Sync (online targets) ────────────────────────────────────────────────

    public static void syncBack(Inventory mirror, Player target) {
        PlayerInventory inv = target.getInventory();

        for (int i = 0; i < 36; i++) {
            inv.setItem(i, mirror.getItem(i));
        }

        inv.setHelmet(    realOrNull(mirror.getItem(SLOT_HELMET)));
        inv.setChestplate(realOrNull(mirror.getItem(SLOT_CHEST)));
        inv.setLeggings(  realOrNull(mirror.getItem(SLOT_LEGS)));
        inv.setBoots(     realOrNull(mirror.getItem(SLOT_BOOTS)));
        inv.setItemInOffHand(itemOrAir(realOrNull(mirror.getItem(SLOT_OFFHAND))));
    }

    /** Extracts the 41-slot array from a closed mirror inventory, respecting placeholders. */
    public static ItemStack[] extractFromMirror(Inventory mirror) {
        ItemStack[] slots = new ItemStack[InventoryDataManager.SLOT_COUNT];
        for (int i = 0; i < 36; i++) slots[i] = mirror.getItem(i);
        slots[36] = realOrNull(mirror.getItem(SLOT_HELMET));
        slots[37] = realOrNull(mirror.getItem(SLOT_CHEST));
        slots[38] = realOrNull(mirror.getItem(SLOT_LEGS));
        slots[39] = realOrNull(mirror.getItem(SLOT_BOOTS));
        slots[40] = realOrNull(mirror.getItem(SLOT_OFFHAND));
        return slots;
    }

    private static ItemStack realOrNull(ItemStack item) {
        return isPlaceholder(item) ? null : item;
    }

    private static ItemStack itemOrAir(ItemStack item) {
        return item != null ? item : new ItemStack(Material.AIR);
    }
}

package dev.leeroy.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ChatColorCommand implements CommandExecutor {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "» Chat Color «";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("bob.chatcolor")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use chat colors.");
            return true;
        }

        openGUI(player);
        return true;
    }

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Row 1 — colors
        setSlot(inv, 1,  Material.WHITE_WOOL,        ChatColor.WHITE        + "White",        ChatColor.WHITE);
        setSlot(inv, 2,  Material.YELLOW_WOOL,       ChatColor.YELLOW       + "Yellow",       ChatColor.YELLOW);
        setSlot(inv, 3,  Material.GOLD_BLOCK,        ChatColor.GOLD         + "Gold",         ChatColor.GOLD);
        setSlot(inv, 4,  Material.ORANGE_WOOL,       ChatColor.RED          + "Red",          ChatColor.RED);
        setSlot(inv, 5,  Material.MAGENTA_WOOL,      ChatColor.LIGHT_PURPLE + "Light Purple", ChatColor.LIGHT_PURPLE);
        setSlot(inv, 6,  Material.PURPLE_WOOL,       ChatColor.DARK_PURPLE  + "Dark Purple",  ChatColor.DARK_PURPLE);
        setSlot(inv, 7,  Material.BLUE_WOOL,         ChatColor.BLUE         + "Blue",         ChatColor.BLUE);

        // Row 2 — more colors
        setSlot(inv, 10, Material.CYAN_WOOL,         ChatColor.AQUA         + "Aqua",         ChatColor.AQUA);
        setSlot(inv, 11, Material.LIGHT_BLUE_WOOL,   ChatColor.DARK_AQUA    + "Dark Aqua",    ChatColor.DARK_AQUA);
        setSlot(inv, 12, Material.LIME_WOOL,         ChatColor.GREEN        + "Green",        ChatColor.GREEN);
        setSlot(inv, 13, Material.GREEN_WOOL,        ChatColor.DARK_GREEN   + "Dark Green",   ChatColor.DARK_GREEN);
        setSlot(inv, 14, Material.GRAY_WOOL,         ChatColor.GRAY         + "Gray",         ChatColor.GRAY);
        setSlot(inv, 15, Material.LIGHT_GRAY_WOOL,   ChatColor.DARK_GRAY    + "Dark Gray",    ChatColor.DARK_GRAY);
        setSlot(inv, 16, Material.BLACK_WOOL,        ChatColor.BLACK        + "Black",        ChatColor.BLACK);

        // Row 3 — special + reset
        setSlot(inv, 19, Material.NETHER_STAR,       ChatColor.BOLD         + "" + ChatColor.WHITE + "Bold",         null);
        setSlot(inv, 20, Material.BOOK,              ChatColor.ITALIC       + "Italic",       null);
        setSlot(inv, 21, Material.NAME_TAG,          ChatColor.UNDERLINE    + "Underline",    null);
        setSlot(inv, 22, Material.BARRIER,           ChatColor.RED          + "Reset Color",  null);

        player.openInventory(inv);
    }

    private static void setSlot(Inventory inv, int slot, Material mat,
                                String name, ChatColor color) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(
                ChatColor.GRAY + "Click to set your chat color",
                color != null
                        ? color + "Preview: Hello!"
                        : ChatColor.WHITE + "Preview: " + ChatColor.BOLD + "Hello!"
        ));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}
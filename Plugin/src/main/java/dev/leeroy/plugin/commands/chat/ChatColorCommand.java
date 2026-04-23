package dev.leeroy.plugin.commands.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChatColorCommand implements BasicCommand {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "» Chat Color «";

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.prefixed(Component.text("Only players can use this command.", NamedTextColor.RED)));
            return;
        }

        if (!player.hasPermission("bob.chatcolor")) {
            player.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to use chat colors.", NamedTextColor.RED)));
            return;
        }

        openGUI(player);
    }

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);

        addSlot(inv, player,  1, Material.WHITE_WOOL,       ChatColor.WHITE        + "White",        "white",        ChatColor.WHITE.toString()        + "Hello!");
        addSlot(inv, player,  2, Material.YELLOW_WOOL,      ChatColor.YELLOW       + "Yellow",       "yellow",       ChatColor.YELLOW.toString()       + "Hello!");
        addSlot(inv, player,  3, Material.GOLD_BLOCK,       ChatColor.GOLD         + "Gold",         "gold",         ChatColor.GOLD.toString()         + "Hello!");
        addSlot(inv, player,  4, Material.ORANGE_WOOL,      ChatColor.RED          + "Red",          "red",          ChatColor.RED.toString()          + "Hello!");
        addSlot(inv, player,  5, Material.MAGENTA_WOOL,     ChatColor.LIGHT_PURPLE + "Light Purple", "light_purple", ChatColor.LIGHT_PURPLE.toString()  + "Hello!");
        addSlot(inv, player,  6, Material.PURPLE_WOOL,      ChatColor.DARK_PURPLE  + "Dark Purple",  "dark_purple",  ChatColor.DARK_PURPLE.toString()   + "Hello!");
        addSlot(inv, player,  7, Material.BLUE_WOOL,        ChatColor.BLUE         + "Blue",         "blue",         ChatColor.BLUE.toString()          + "Hello!");
        addSlot(inv, player, 10, Material.CYAN_WOOL,        ChatColor.AQUA         + "Aqua",         "aqua",         ChatColor.AQUA.toString()          + "Hello!");
        addSlot(inv, player, 11, Material.LIGHT_BLUE_WOOL,  ChatColor.DARK_AQUA    + "Dark Aqua",    "dark_aqua",    ChatColor.DARK_AQUA.toString()     + "Hello!");
        addSlot(inv, player, 12, Material.LIME_WOOL,        ChatColor.GREEN        + "Green",        "green",        ChatColor.GREEN.toString()         + "Hello!");
        addSlot(inv, player, 13, Material.GREEN_WOOL,       ChatColor.DARK_GREEN   + "Dark Green",   "dark_green",   ChatColor.DARK_GREEN.toString()    + "Hello!");
        addSlot(inv, player, 14, Material.GRAY_WOOL,        ChatColor.GRAY         + "Gray",         "gray",         ChatColor.GRAY.toString()          + "Hello!");
        addSlot(inv, player, 15, Material.LIGHT_GRAY_WOOL,  ChatColor.DARK_GRAY    + "Dark Gray",    "dark_gray",    ChatColor.DARK_GRAY.toString()     + "Hello!");
        addSlot(inv, player, 16, Material.BLACK_WOOL,       ChatColor.BLACK        + "Black",        "black",        ChatColor.BLACK.toString()         + "Hello!");

        addSlot(inv, player, 19, Material.NETHER_STAR,      ChatColor.WHITE + "" + ChatColor.BOLD      + "Bold",      "bold",      ChatColor.BOLD.toString()      + "Hello!");
        addSlot(inv, player, 20, Material.BOOK,             ChatColor.WHITE + "" + ChatColor.ITALIC    + "Italic",    "italic",    ChatColor.ITALIC.toString()    + "Hello!");
        addSlot(inv, player, 21, Material.NAME_TAG,         ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Underline", "underline", ChatColor.UNDERLINE.toString() + "Hello!");

        addSlot(inv, player, 22, Material.DIAMOND,
                "" + ChatColor.RED + "R" + ChatColor.GOLD + "a" + ChatColor.YELLOW + "i"
                        + ChatColor.GREEN + "n" + ChatColor.AQUA + "b" + ChatColor.BLUE + "o"
                        + ChatColor.LIGHT_PURPLE + "w",
                "rainbow",
                "" + ChatColor.RED + "H" + ChatColor.GOLD + "e" + ChatColor.YELLOW + "l"
                        + ChatColor.GREEN + "l" + ChatColor.AQUA + "o" + ChatColor.BLUE + "!");

        addSlot(inv, player, 31, Material.BARRIER,
                ChatColor.RED + "Reset Color", "reset", ChatColor.GRAY + "Remove your chat color.");

        player.openInventory(inv);
    }

    private static void addSlot(Inventory inv, Player player, int slot, Material mat,
                                String name, String permKey, String preview) {
        boolean canUse = permKey.equals("reset") || player.hasPermission("bob.chatcolor." + permKey);

        ItemStack item = new ItemStack(canUse ? mat : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(canUse ? name : ChatColor.DARK_GRAY + "» " + ChatColor.stripColor(name));

        List<String> lore = new ArrayList<>();
        if (canUse) {
            lore.add(ChatColor.GRAY + "Preview: " + preview);
            lore.add(ChatColor.YELLOW + "Click to select!");
        } else {
            lore.add(ChatColor.RED + "No permission.");
            lore.add(ChatColor.DARK_GRAY + "bob.chatcolor." + permKey);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}

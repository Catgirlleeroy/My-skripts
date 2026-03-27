package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.commands.ChatColorCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatColorListener implements Listener {

    // Stores each player's chosen chat color/format
    private final Map<UUID, String> chatColors = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(ChatColorCommand.GUI_TITLE)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        Material mat = event.getCurrentItem().getType();

        String colorCode = resolveColor(mat);
        if (colorCode == null) return;

        if (mat == Material.BARRIER) {
            // Reset
            chatColors.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GRAY + "✦ Your chat color has been " + ChatColor.WHITE + "reset" + ChatColor.GRAY + ".");
        } else {
            chatColors.put(player.getUniqueId(), colorCode);
            player.sendMessage(ChatColor.GRAY + "✦ Chat color set to: " + colorCode + "This color!");
        }

        player.closeInventory();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String color = chatColors.get(event.getPlayer().getUniqueId());
        if (color != null) {
            event.setMessage(color + event.getMessage());
        }
    }

    private String resolveColor(Material mat) {
        return switch (mat) {
            case WHITE_WOOL       -> ChatColor.WHITE.toString();
            case YELLOW_WOOL      -> ChatColor.YELLOW.toString();
            case GOLD_BLOCK       -> ChatColor.GOLD.toString();
            case ORANGE_WOOL      -> ChatColor.RED.toString();
            case MAGENTA_WOOL     -> ChatColor.LIGHT_PURPLE.toString();
            case PURPLE_WOOL      -> ChatColor.DARK_PURPLE.toString();
            case BLUE_WOOL        -> ChatColor.BLUE.toString();
            case CYAN_WOOL        -> ChatColor.AQUA.toString();
            case LIGHT_BLUE_WOOL  -> ChatColor.DARK_AQUA.toString();
            case LIME_WOOL        -> ChatColor.GREEN.toString();
            case GREEN_WOOL       -> ChatColor.DARK_GREEN.toString();
            case GRAY_WOOL        -> ChatColor.GRAY.toString();
            case LIGHT_GRAY_WOOL  -> ChatColor.DARK_GRAY.toString();
            case BLACK_WOOL       -> ChatColor.BLACK.toString();
            case NETHER_STAR      -> ChatColor.BOLD.toString();
            case BOOK             -> ChatColor.ITALIC.toString();
            case NAME_TAG         -> ChatColor.UNDERLINE.toString();
            case BARRIER          -> "reset";
            default               -> null;
        };
    }
}
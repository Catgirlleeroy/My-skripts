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

    private static final ChatColor[] RAINBOW = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE
    };

    // Stores color code string, or "rainbow" for rainbow mode
    private final Map<UUID, String> chatColors = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(ChatColorCommand.GUI_TITLE)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Material mat = event.getCurrentItem().getType();
        if (mat == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage(ChatColor.RED + "You don't have permission for that color.");
            return;
        }

        String permKey = resolvePermKey(mat);
        if (permKey == null) return;

        if (permKey.equals("reset")) {
            chatColors.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GRAY + "✦ Your chat color has been " + ChatColor.WHITE + "reset" + ChatColor.GRAY + ".");
            player.closeInventory();
            return;
        }

        if (!player.hasPermission("bob.chatcolor." + permKey)) {
            player.sendMessage(ChatColor.RED + "You don't have permission for that color.");
            return;
        }

        String colorCode = resolveColorCode(mat);
        chatColors.put(player.getUniqueId(), colorCode);

        String preview = colorCode.equals("rainbow") ? buildRainbow("This color!") : colorCode + "This color!";
        player.sendMessage(ChatColor.GRAY + "✦ Chat color set to: " + preview);
        player.closeInventory();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String color = chatColors.get(event.getPlayer().getUniqueId());
        if (color == null) return;

        if (color.equals("rainbow")) {
            event.setMessage(buildRainbow(event.getMessage()));
        } else {
            event.setMessage(color + event.getMessage());
        }
    }

    // ── Rainbow builder — cycles color per character ─────────────────────────

    private String buildRainbow(String message) {
        StringBuilder sb = new StringBuilder();
        int colorIndex = 0;
        for (char c : message.toCharArray()) {
            if (c == ' ') {
                sb.append(' ');
            } else {
                sb.append(RAINBOW[colorIndex % RAINBOW.length]).append(c);
                colorIndex++;
            }
        }
        return sb.toString();
    }

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private String resolvePermKey(Material mat) {
        return switch (mat) {
            case WHITE_WOOL       -> "white";
            case YELLOW_WOOL      -> "yellow";
            case GOLD_BLOCK       -> "gold";
            case ORANGE_WOOL      -> "red";
            case MAGENTA_WOOL     -> "light_purple";
            case PURPLE_WOOL      -> "dark_purple";
            case BLUE_WOOL        -> "blue";
            case CYAN_WOOL        -> "aqua";
            case LIGHT_BLUE_WOOL  -> "dark_aqua";
            case LIME_WOOL        -> "green";
            case GREEN_WOOL       -> "dark_green";
            case GRAY_WOOL        -> "gray";
            case LIGHT_GRAY_WOOL  -> "dark_gray";
            case BLACK_WOOL       -> "black";
            case NETHER_STAR      -> "bold";
            case BOOK             -> "italic";
            case NAME_TAG         -> "underline";
            case DIAMOND          -> "rainbow";
            case BARRIER          -> "reset";
            default               -> null;
        };
    }

    private String resolveColorCode(Material mat) {
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
            case DIAMOND          -> "rainbow";
            default               -> null;
        };
    }
}
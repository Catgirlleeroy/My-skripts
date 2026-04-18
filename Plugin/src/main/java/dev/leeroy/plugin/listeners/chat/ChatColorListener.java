package dev.leeroy.plugin.listeners.chat;

import dev.leeroy.plugin.commands.chat.ChatColorCommand;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatColorListener implements Listener {

    private static final NamedTextColor[] RAINBOW = {
            NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW,
            NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.LIGHT_PURPLE
    };

    // Stores legacy §X color code string, or "rainbow"
    private final Map<UUID, String> chatColors = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(ChatColorCommand.GUI_TITLE)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Material mat = event.getCurrentItem().getType();
        if (mat == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage(Component.text("You don't have permission for that color.", NamedTextColor.RED));
            return;
        }

        String permKey = resolvePermKey(mat);
        if (permKey == null) return;

        if (permKey.equals("reset")) {
            chatColors.remove(player.getUniqueId());
            player.sendMessage(Component.text("✦ Your chat color has been reset.", NamedTextColor.GRAY));
            player.closeInventory();
            return;
        }

        if (!player.hasPermission("bob.chatcolor." + permKey)) {
            player.sendMessage(Component.text("You don't have permission for that color.", NamedTextColor.RED));
            return;
        }

        String colorCode = resolveColorCode(mat);
        chatColors.put(player.getUniqueId(), colorCode);

        Component preview = colorCode.equals("rainbow")
                ? buildRainbowComponent("This color!")
                : LegacyComponentSerializer.legacySection().deserialize(colorCode + "This color!");
        player.sendMessage(Component.text("✦ Chat color set to: ", NamedTextColor.GRAY).append(preview));
        player.closeInventory();
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        String color = chatColors.get(event.getPlayer().getUniqueId());
        if (color == null) return;

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (color.equals("rainbow")) {
            event.message(buildRainbowComponent(plain));
        } else {
            event.message(LegacyComponentSerializer.legacySection().deserialize(color + plain));
        }
    }

    // ── Rainbow builder — cycles color per character ─────────────────────────

    private Component buildRainbowComponent(String message) {
        Component result = Component.empty();
        int colorIndex = 0;
        for (char c : message.toCharArray()) {
            if (c == ' ') {
                result = result.append(Component.text(' '));
            } else {
                result = result.append(Component.text(c).color(RAINBOW[colorIndex % RAINBOW.length]));
                colorIndex++;
            }
        }
        return result;
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

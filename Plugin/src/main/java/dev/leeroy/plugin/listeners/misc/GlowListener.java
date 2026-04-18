package dev.leeroy.plugin.listeners.misc;

import dev.leeroy.plugin.commands.misc.GlowCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlowListener implements Listener {

    private static final ChatColor[] RAINBOW = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE
    };

    private final Map<UUID, BukkitTask> rainbowTasks = new HashMap<>();
    private final JavaPlugin plugin;

    public GlowListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(GlowCommand.GUI_TITLE)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType().isAir()) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        Material mat = event.getCurrentItem().getType();
        if (mat == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage(Component.text("You don't have permission for that glow color.", NamedTextColor.RED));
            return;
        }

        String permKey = resolvePermKey(mat);
        if (permKey == null) return;

        stopRainbow(player);

        if (permKey.equals("reset")) {
            GlowCommand.removeGlow(player);
            player.sendMessage(Component.text("✦ Your glow has been ", NamedTextColor.GRAY)
                    .append(Component.text("disabled", NamedTextColor.WHITE))
                    .append(Component.text(".", NamedTextColor.GRAY)));
            player.closeInventory();
            return;
        }

        if (!player.hasPermission("bob.glow." + permKey)) {
            player.sendMessage(Component.text("You don't have permission for that glow color.", NamedTextColor.RED));
            return;
        }

        if (permKey.equals("rainbow")) {
            startRainbow(player);
            player.sendMessage(Component.text("✦ ", NamedTextColor.RED)
                    .append(Component.text("R", NamedTextColor.RED))
                    .append(Component.text("a", NamedTextColor.GOLD))
                    .append(Component.text("i", NamedTextColor.YELLOW))
                    .append(Component.text("n", NamedTextColor.GREEN))
                    .append(Component.text("b", NamedTextColor.AQUA))
                    .append(Component.text("o", NamedTextColor.BLUE))
                    .append(Component.text("w", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" glow enabled!", NamedTextColor.GRAY)));
        } else {
            ChatColor color = resolveColor(mat);
            if (color == null) return;
            GlowCommand.applyGlow(player, color);
            player.sendMessage(Component.text("✦ Glow color set to: " + permKey.replace("_", " ") + "!", NamedTextColor.GRAY));
        }

        player.closeInventory();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopRainbow(player);
        GlowCommand.removeGlow(player);
    }

    private void startRainbow(Player player) {
        final int[] index = {0};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopRainbow(player);
                return;
            }
            GlowCommand.applyGlow(player, RAINBOW[index[0] % RAINBOW.length]);
            index[0]++;
        }, 0L, 20L);

        rainbowTasks.put(player.getUniqueId(), task);
    }

    private void stopRainbow(Player player) {
        BukkitTask task = rainbowTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

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
            case DIAMOND          -> "rainbow";
            case BARRIER          -> "reset";
            default               -> null;
        };
    }

    private ChatColor resolveColor(Material mat) {
        return switch (mat) {
            case WHITE_WOOL       -> ChatColor.WHITE;
            case YELLOW_WOOL      -> ChatColor.YELLOW;
            case GOLD_BLOCK       -> ChatColor.GOLD;
            case ORANGE_WOOL      -> ChatColor.RED;
            case MAGENTA_WOOL     -> ChatColor.LIGHT_PURPLE;
            case PURPLE_WOOL      -> ChatColor.DARK_PURPLE;
            case BLUE_WOOL        -> ChatColor.BLUE;
            case CYAN_WOOL        -> ChatColor.AQUA;
            case LIGHT_BLUE_WOOL  -> ChatColor.DARK_AQUA;
            case LIME_WOOL        -> ChatColor.GREEN;
            case GREEN_WOOL       -> ChatColor.DARK_GREEN;
            case GRAY_WOOL        -> ChatColor.GRAY;
            case LIGHT_GRAY_WOOL  -> ChatColor.DARK_GRAY;
            case BLACK_WOOL       -> ChatColor.BLACK;
            default               -> null;
        };
    }
}

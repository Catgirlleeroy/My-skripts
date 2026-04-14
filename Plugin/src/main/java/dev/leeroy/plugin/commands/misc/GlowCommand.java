package dev.leeroy.plugin.commands.misc;

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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class GlowCommand implements CommandExecutor {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "» Glow Color «";

    // Team name prefix — kept short to avoid scoreboard name limits
    public static final String TEAM_PREFIX = "bob_glow_";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("bob.glow")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use glow.");
            return true;
        }

        openGUI(player);
        return true;
    }

    public static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);

        addSlot(inv, player,  1, Material.WHITE_WOOL,       ChatColor.WHITE        + "White",        "white");
        addSlot(inv, player,  2, Material.YELLOW_WOOL,      ChatColor.YELLOW       + "Yellow",       "yellow");
        addSlot(inv, player,  3, Material.GOLD_BLOCK,       ChatColor.GOLD         + "Gold",         "gold");
        addSlot(inv, player,  4, Material.ORANGE_WOOL,      ChatColor.RED          + "Red",          "red");
        addSlot(inv, player,  5, Material.MAGENTA_WOOL,     ChatColor.LIGHT_PURPLE + "Light Purple", "light_purple");
        addSlot(inv, player,  6, Material.PURPLE_WOOL,      ChatColor.DARK_PURPLE  + "Dark Purple",  "dark_purple");
        addSlot(inv, player,  7, Material.BLUE_WOOL,        ChatColor.BLUE         + "Blue",         "blue");
        addSlot(inv, player, 10, Material.CYAN_WOOL,        ChatColor.AQUA         + "Aqua",         "aqua");
        addSlot(inv, player, 11, Material.LIGHT_BLUE_WOOL,  ChatColor.DARK_AQUA    + "Dark Aqua",    "dark_aqua");
        addSlot(inv, player, 12, Material.LIME_WOOL,        ChatColor.GREEN        + "Green",        "green");
        addSlot(inv, player, 13, Material.GREEN_WOOL,       ChatColor.DARK_GREEN   + "Dark Green",   "dark_green");
        addSlot(inv, player, 14, Material.GRAY_WOOL,        ChatColor.GRAY         + "Gray",         "gray");
        addSlot(inv, player, 15, Material.LIGHT_GRAY_WOOL,  ChatColor.DARK_GRAY    + "Dark Gray",    "dark_gray");
        addSlot(inv, player, 16, Material.BLACK_WOOL,       ChatColor.BLACK        + "Black",        "black");

        // Rainbow
        addSlot(inv, player, 22, Material.DIAMOND,
                "" + ChatColor.RED + "R" + ChatColor.GOLD + "a" + ChatColor.YELLOW + "i"
                        + ChatColor.GREEN + "n" + ChatColor.AQUA + "b" + ChatColor.BLUE + "o"
                        + ChatColor.LIGHT_PURPLE + "w",
                "rainbow");

        // Reset/disable glow
        addSlot(inv, player, 31, Material.BARRIER, ChatColor.RED + "Disable Glow", "reset");

        player.openInventory(inv);
    }

    private static void addSlot(Inventory inv, Player player, int slot,
                                Material mat, String name, String permKey) {
        boolean canUse = permKey.equals("reset") || player.hasPermission("bob.glow." + permKey);

        ItemStack item = new ItemStack(canUse ? mat : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(canUse ? name : ChatColor.DARK_GRAY + "» " + ChatColor.stripColor(name));

        List<String> lore = new ArrayList<>();
        if (canUse) {
            lore.add(ChatColor.GRAY + "Click to set your glow color!");
        } else {
            lore.add(ChatColor.RED + "No permission.");
            lore.add(ChatColor.DARK_GRAY + "bob.glow." + permKey);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    // ── Scoreboard team helpers ───────────────────────────────────────────────

    /**
     * Applies a glow color to a player using a scoreboard team.
     * ChatColor must be a color (not a format).
     */
    public static void applyGlow(Player player, ChatColor color) {
        Scoreboard board = getOrCreateBoard();
        String teamName = TEAM_PREFIX + color.name().toLowerCase();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(color);
        }

        // Remove from any existing glow team first
        removeFromAllGlowTeams(board, player);

        team.addEntry(player.getName());
        player.setGlowing(true);
        Bukkit.getOnlinePlayers().forEach(p -> p.setScoreboard(board));
    }

    public static void removeGlow(Player player) {
        Scoreboard board = getOrCreateBoard();
        removeFromAllGlowTeams(board, player);
        player.setGlowing(false);
    }

    private static void removeFromAllGlowTeams(Scoreboard board, Player player) {
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
    }

    private static Scoreboard getOrCreateBoard() {
        // Use the main scoreboard so it's shared across all players
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }
}
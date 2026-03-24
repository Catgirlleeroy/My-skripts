package dev.leeroy.plugin.Utils;

import org.bukkit.entity.Player;

public class PlayerHealer {

    // Utility class — no instantiation needed
    private PlayerHealer() {}

    public static void heal(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }
}
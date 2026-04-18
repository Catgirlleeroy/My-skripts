package dev.leeroy.plugin.Utils.misc;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TabUtil {

    private TabUtil() {}

    /** Returns the argument at position i, or "" if the args array is shorter. */
    public static String arg(String[] args, int i) {
        return i < args.length ? args[i] : "";
    }

    public static Collection<String> onlinePlayers(CommandSourceStack stack, String input, VanishManager vanishManager) {
        CommandSender sender = stack.getSender();
        boolean canSeeVanished = sender.hasPermission("bob.vanish.see");
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (vanishManager.isVanished(p.getUniqueId()) && !canSeeVanished) continue;
            if (p.getName().toLowerCase().startsWith(lower)) result.add(p.getName());
        }
        return result;
    }

    public static Collection<String> cachedPlayers(PlayerCache playerCache, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String name : playerCache.getAllNames()) {
            if (name.toLowerCase().startsWith(lower)) result.add(name);
        }
        return result;
    }
}
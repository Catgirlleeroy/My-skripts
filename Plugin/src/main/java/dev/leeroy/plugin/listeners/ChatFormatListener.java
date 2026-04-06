package dev.leeroy.plugin.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatListener implements Listener {

    // Matches &#RRGGBB hex color codes
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final JavaPlugin plugin;
    private LuckPerms luckPerms;

    public ChatFormatListener(JavaPlugin plugin) {
        this.plugin = plugin;

        RegisteredServiceProvider<LuckPerms> provider =
                plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);

        if (provider != null) {
            luckPerms = provider.getProvider();
            plugin.getLogger().info("[Bob] LuckPerms hooked successfully for chat formatting.");
        } else {
            plugin.getLogger().warning("[Bob] LuckPerms not found — chat will show without group prefix.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-format.enabled", true)) return;

        Player player = event.getPlayer();

        String prefix    = "";
        String groupName = "";

        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                CachedMetaData meta = user.getCachedData().getMetaData();
                String raw = meta.getPrefix();
                prefix = raw != null ? colorize(raw) : "";
                groupName = user.getPrimaryGroup();
            }
        }

        String format = plugin.getConfig().getString(
                "chat-format.format",
                "{prefix} &f{player}&7: &f{message}"
        );

        format = format
                .replace("{prefix}",  prefix)
                .replace("{group}",   groupName)
                .replace("{name}",    player.getName())
                .replace("{player}",  player.getDisplayName());

        format = colorize(format);
        format = format.replace("{message}", "%2$s");

        if (!format.contains("%2$s")) {
            format = format + " %2$s";
        }

        event.setFormat(format);
    }

    /**
     * Translates both &#RRGGBB hex codes and legacy &x color codes.
     */
    private String colorize(String input) {
        // Step 1: translate &#RRGGBB → BungeeCord ChatColor hex
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(sb);

        // Step 2: translate legacy &x codes
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
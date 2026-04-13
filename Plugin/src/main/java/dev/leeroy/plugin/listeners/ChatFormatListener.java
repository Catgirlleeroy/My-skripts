package dev.leeroy.plugin.listeners;

import dev.leeroy.plugin.Utils.BobHooks;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatListener implements Listener {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final JavaPlugin plugin;

    public ChatFormatListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-format.enabled", true)) return;

        Player player = event.getPlayer();

        String prefix    = "";
        String groupName = "";

        if (BobHooks.hasVault()) {
            prefix    = VaultHook.getPrefix(player);
            groupName = VaultHook.getGroup(player);
        }

        String format = plugin.getConfig().getString(
                "chat-format.format",
                "{prefix}&f {player}&7: &f{message}"
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

    private String colorize(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
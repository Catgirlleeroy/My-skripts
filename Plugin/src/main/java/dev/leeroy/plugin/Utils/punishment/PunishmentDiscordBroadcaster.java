package dev.leeroy.plugin.Utils.punishment;

import dev.leeroy.plugin.Utils.misc.BobHooks;
import dev.leeroy.plugin.Utils.misc.MessagesConfig;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.plugin.java.JavaPlugin;

public class PunishmentDiscordBroadcaster {

    private final JavaPlugin plugin;
    private final MessagesConfig messagesConfig;

    public PunishmentDiscordBroadcaster(JavaPlugin plugin, MessagesConfig messagesConfig) {
        this.plugin         = plugin;
        this.messagesConfig = messagesConfig;
    }

    public void broadcast(String type, String player, String staff, String reason, String duration) {
        send(type, player, staff, reason, duration, -1, -1);
    }

    public void broadcastWarn(String type, String player, String staff, String reason, int warns, int max) {
        send(type, player, staff, reason, null, warns, max);
    }

    // Removal actions show green; everything else shows red
    private static final java.util.Set<String> REMOVAL_TYPES =
            java.util.Set.of("unban", "ipunban", "unmute", "unwarn");

    private void send(String type, String player, String staff, String reason, String duration, int warns, int max) {
        if (!BobHooks.hasDiscordSRV()) return;
        if (!plugin.getConfig().getBoolean("discord.enabled", false)) return;

        String channelNameOrId = plugin.getConfig().getString("discord.channel", "global");
        String template        = messagesConfig.get().getString("discord.messages." + type, "");
        if (template == null || template.isEmpty()) return;

        String description = template
                .replace("{player}",   player   != null ? player              : "")
                .replace("{staff}",    staff    != null ? staff               : "")
                .replace("{reason}",   reason   != null ? reason              : "")
                .replace("{duration}", duration != null ? duration            : "")
                .replace("{warns}",    warns >= 0       ? String.valueOf(warns) : "")
                .replace("{max}",      max   >= 0       ? String.valueOf(max)   : "");

        int colorInt = REMOVAL_TYPES.contains(type) ? 0x57F287 : 0xED4245;

        try {
            var dsrv = DiscordSRV.getPlugin();
            Object channel = dsrv.getClass()
                    .getMethod("getDestinationTextChannelForGameChannelName", String.class)
                    .invoke(dsrv, channelNameOrId);
            if (channel == null) {
                Object guild = dsrv.getClass().getMethod("getMainGuild").invoke(dsrv);
                if (guild != null)
                    channel = guild.getClass().getMethod("getTextChannelById", String.class)
                            .invoke(guild, channelNameOrId);
            }
            if (channel == null) return;

            ClassLoader cl = channel.getClass().getClassLoader();
            Class<?> ebClass = cl.loadClass("github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder");
            Object eb = ebClass.getDeclaredConstructor().newInstance();
            ebClass.getMethod("setDescription", CharSequence.class).invoke(eb, description);
            ebClass.getMethod("setColor", java.awt.Color.class).invoke(eb, new java.awt.Color(colorInt));
            Object embed = ebClass.getMethod("build").invoke(eb);

            Class<?> embedClass = cl.loadClass("github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed");
            Object restAction = channel.getClass().getMethod("sendMessage", embedClass).invoke(channel, embed);
            restAction.getClass().getMethod("queue").invoke(restAction);
        } catch (java.lang.reflect.InvocationTargetException e) {
            plugin.getLogger().warning("[Bob] Discord broadcast failed: " + e.getCause());
        } catch (Exception e) {
            plugin.getLogger().warning("[Bob] Discord broadcast failed: " + e);
        }
    }
}

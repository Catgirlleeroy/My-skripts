package dev.leeroy.plugin.Utils.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

public final class TextUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {}

    /** Parses a string with & color codes into an Adventure Component. */
    public static Component parse(String s) {
        return s == null ? Component.empty() : LEGACY.deserialize(s);
    }

    /** Broadcasts a & color-coded message to all players and console. */
    public static void broadcast(String msg) {
        Bukkit.broadcast(parse(msg));
    }
}

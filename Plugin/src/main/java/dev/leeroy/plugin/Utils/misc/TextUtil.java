package dev.leeroy.plugin.Utils.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern LEGACY_HEX  = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    private static JavaPlugin plugin;

    private TextUtil() {}

    public static void init(JavaPlugin p) { plugin = p; }

    public static String rawPrefix() {
        if (plugin == null) return "";
        return plugin.getConfig().getString("prefix", "&8[&6Bob&8] &r");
    }

    public static Component prefix() { return parse(rawPrefix()); }

    /** Prefix + parsed string. */
    public static Component prefixed(String msg) { return prefix().append(parse(msg)); }

    /** Prefix + existing Component. */
    public static Component prefixed(Component msg) { return prefix().append(msg); }

    /**
     * Parses legacy &-codes, legacy &#RRGGBB hex, and MiniMessage tags.
     * All three formats can be mixed in the same string.
     */
    public static Component parse(String s) {
        if (s == null) return Component.empty();
        // &#RRGGBB → <#RRGGBB>
        s = LEGACY_HEX.matcher(s).replaceAll("<#$1>");
        // &a → <green>, &r → <reset>, etc.
        s = LEGACY_CODE.matcher(s).replaceAll(m -> switch (Character.toLowerCase(m.group(1).charAt(0))) {
            case '0' -> "<black>";         case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";    case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";      case '5' -> "<dark_purple>";
            case '6' -> "<gold>";          case '7' -> "<gray>";
            case '8' -> "<dark_gray>";     case '9' -> "<blue>";
            case 'a' -> "<green>";         case 'b' -> "<aqua>";
            case 'c' -> "<red>";           case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";        case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";    case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>"; case 'n' -> "<underlined>";
            case 'o' -> "<italic>";        case 'r' -> "<reset>";
            default  -> m.group(0);
        });
        return MM.deserialize(s);
    }

    /** Broadcasts to all players and console. */
    public static void broadcast(String msg) {
        Bukkit.broadcast(parse(msg));
    }
}

package dev.leeroy.plugin.listeners;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Isolated class containing all Vault Chat API references.
 * Java only loads this class when it's actually called, so if Vault
 * is not installed and ChatFormatListener never calls into this class,
 * no ClassNotFoundException is thrown.
 */
public class VaultHook {

    private static Chat chat;

    static {
        RegisteredServiceProvider<Chat> provider =
                Bukkit.getServicesManager().getRegistration(Chat.class);
        if (provider != null) {
            chat = provider.getProvider();
        }
    }

    /**
     * Returns the chat prefix for the player, or an empty string if unavailable.
     */
    public static String getPrefix(Player player) {
        if (chat == null) return "";
        String prefix = chat.getPlayerPrefix(player);
        return prefix != null ? prefix : "";
    }

    /**
     * Returns the primary group name for the player, or an empty string if unavailable.
     */
    public static String getGroup(Player player) {
        if (chat == null) return "";
        String group = chat.getPrimaryGroup(player);
        return group != null ? group : "";
    }
}
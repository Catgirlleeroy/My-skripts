package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.AutoMessageManager;
import dev.leeroy.plugin.Utils.BanManager;
import dev.leeroy.plugin.Utils.ChatGameManager;
import dev.leeroy.plugin.Utils.IPBanManager;
import dev.leeroy.plugin.Utils.PunishConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final BanManager banManager;
    private final IPBanManager ipBanManager;
    private final PunishConfig punishConfig;
    private final AutoMessageManager autoMessageManager;
    private final ChatGameManager chatGameManager;

    public ReloadCommand(JavaPlugin plugin, BanManager banManager,
                         IPBanManager ipBanManager, PunishConfig punishConfig,
                         AutoMessageManager autoMessageManager, ChatGameManager chatGameManager) {
        this.plugin             = plugin;
        this.banManager         = banManager;
        this.ipBanManager       = ipBanManager;
        this.punishConfig       = punishConfig;
        this.autoMessageManager = autoMessageManager;
        this.chatGameManager    = chatGameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload Bob.");
            return true;
        }

        plugin.reloadConfig();
        banManager.reload();
        ipBanManager.reload();
        punishConfig.reload();
        autoMessageManager.restart();
        chatGameManager.restart();

        sender.sendMessage(ChatColor.GREEN + "[Bob] Reloaded all config files and restarted tasks.");
        plugin.getLogger().info("Bob reloaded by " + sender.getName());
        return true;
    }
}
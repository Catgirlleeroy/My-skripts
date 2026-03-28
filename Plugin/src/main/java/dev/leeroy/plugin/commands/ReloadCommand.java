package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.BanManager;
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

    public ReloadCommand(JavaPlugin plugin, BanManager banManager,
                         IPBanManager ipBanManager, PunishConfig punishConfig) {
        this.plugin       = plugin;
        this.banManager   = banManager;
        this.ipBanManager = ipBanManager;
        this.punishConfig = punishConfig;
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

        sender.sendMessage(ChatColor.GREEN + "[Bob] Reloaded config.yml, punish.yml, bans.yml and ipbans.yml from disk.");
        plugin.getLogger().info("Bob reloaded by " + sender.getName());
        return true;
    }
}
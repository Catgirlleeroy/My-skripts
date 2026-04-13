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

import java.util.ArrayList;
import java.util.List;

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

        List<String> failed = new ArrayList<>();

        attempt(sender, failed, "config",           () -> plugin.reloadConfig());
        attempt(sender, failed, "ban data",         () -> banManager.reload());
        attempt(sender, failed, "ip-ban data",      () -> ipBanManager.reload());
        attempt(sender, failed, "punish.yml",       () -> punishConfig.reload());
        attempt(sender, failed, "auto-messages",    () -> autoMessageManager.restart());
        attempt(sender, failed, "chat-games",       () -> chatGameManager.restart());

        if (failed.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "✔ Bob reloaded successfully — all components OK.");
            plugin.getLogger().info("Bob reloaded by " + sender.getName() + " — all OK.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "⚠ Bob reloaded with " + failed.size() + " error(s):");
            for (String f : failed) {
                sender.sendMessage(ChatColor.RED + "  ✘ " + f + " failed — check console for details.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Everything else reloaded fine.");
        }

        return true;
    }

    /**
     * Runs a reload task, catches any exception, logs it to console,
     * and adds the component name to the failed list if it threw.
     */
    private void attempt(CommandSender sender, List<String> failed, String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            failed.add(name);
            plugin.getLogger().severe("[Bob] Failed to reload " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
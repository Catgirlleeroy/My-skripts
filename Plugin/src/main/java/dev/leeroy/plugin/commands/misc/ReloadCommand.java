package dev.leeroy.plugin.commands.misc;

import dev.leeroy.plugin.Utils.chat.AutoMessageManager;
import dev.leeroy.plugin.Utils.chat.ChatGameManager;
import dev.leeroy.plugin.Utils.misc.MessagesConfig;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import dev.leeroy.plugin.Utils.punishment.BanManager;
import dev.leeroy.plugin.Utils.punishment.IPBanManager;
import dev.leeroy.plugin.Utils.punishment.PunishConfig;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements BasicCommand {

    private final JavaPlugin plugin;
    private final BanManager banManager;
    private final IPBanManager ipBanManager;
    private final PunishConfig punishConfig;
    private final MessagesConfig messagesConfig;
    private final AutoMessageManager autoMessageManager;
    private final ChatGameManager chatGameManager;

    public ReloadCommand(JavaPlugin plugin, BanManager banManager,
                         IPBanManager ipBanManager, PunishConfig punishConfig,
                         MessagesConfig messagesConfig, AutoMessageManager autoMessageManager,
                         ChatGameManager chatGameManager) {
        this.plugin             = plugin;
        this.banManager         = banManager;
        this.ipBanManager       = ipBanManager;
        this.punishConfig       = punishConfig;
        this.messagesConfig     = messagesConfig;
        this.autoMessageManager = autoMessageManager;
        this.chatGameManager    = chatGameManager;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.reload")) {
            sender.sendMessage(TextUtil.prefixed(Component.text("You don't have permission to reload Bob.", NamedTextColor.RED)));
            return;
        }

        List<String> failed = new ArrayList<>();

        attempt(failed, "config",        () -> plugin.reloadConfig());
        attempt(failed, "messages.yml",  () -> messagesConfig.reload());
        attempt(failed, "ban data",      () -> banManager.reload());
        attempt(failed, "ip-ban data",   () -> ipBanManager.reload());
        attempt(failed, "punish.yml",    () -> punishConfig.reload());
        attempt(failed, "auto-messages", () -> autoMessageManager.restart());
        attempt(failed, "chat-games",    () -> chatGameManager.restart());

        if (failed.isEmpty()) {
            sender.sendMessage(TextUtil.prefixed(Component.text("✔ Bob reloaded successfully — all components OK.", NamedTextColor.GREEN)));
            plugin.getLogger().info("Bob reloaded by " + sender.getName() + " — all OK.");
        } else {
            sender.sendMessage(TextUtil.prefixed(Component.text("⚠ Bob reloaded with " + failed.size() + " error(s):", NamedTextColor.YELLOW)));
            for (String f : failed) {
                sender.sendMessage(TextUtil.prefixed(Component.text("  ✘ " + f + " failed — check console for details.", NamedTextColor.RED)));
            }
            sender.sendMessage(TextUtil.prefixed(Component.text("Everything else reloaded fine.", NamedTextColor.YELLOW)));
        }
    }

    private void attempt(List<String> failed, String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            failed.add(name);
            plugin.getLogger().severe("[Bob] Failed to reload " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

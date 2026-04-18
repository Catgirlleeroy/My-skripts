package dev.leeroy.plugin.commands.chat;

import dev.leeroy.plugin.Utils.chat.ChatGameManager;
import dev.leeroy.plugin.Utils.misc.TextUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ChatGameCommand implements BasicCommand {

    private final ChatGameManager chatGameManager;
    private final JavaPlugin plugin;

    public ChatGameCommand(ChatGameManager chatGameManager, JavaPlugin plugin) {
        this.chatGameManager = chatGameManager;
        this.plugin          = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (!sender.hasPermission("bob.chatgame")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(TextUtil.parse("&6── Chat Game Commands ──"));
            sender.sendMessage(TextUtil.parse("&e/chatgame now &7— Start a game immediately"));
            sender.sendMessage(TextUtil.parse("&e/chatgame add <word> &7— Add a word"));
            sender.sendMessage(TextUtil.parse("&e/chatgame remove <word> &7— Remove a word"));
            sender.sendMessage(TextUtil.parse("&e/chatgame list &7— List all words"));
            return;
        }

        switch (args[0].toLowerCase()) {

            case "now" -> {
                if (!plugin.getConfig().getBoolean("chat-games.enabled", true)) {
                    sender.sendMessage(Component.text("Chat games are currently disabled in config.yml.", NamedTextColor.RED));
                    return;
                }
                if (chatGameManager.isActive()) {
                    sender.sendMessage(Component.text("A chat game is already running!", NamedTextColor.RED));
                    return;
                }
                chatGameManager.startGame();
                sender.sendMessage(Component.text("Chat game started!", NamedTextColor.GREEN));
            }

            case "add" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /chatgame add <word>", NamedTextColor.YELLOW)); return; }
                String word = args[1];
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                if (words.stream().anyMatch(w -> w.equalsIgnoreCase(word))) {
                    sender.sendMessage(Component.text("'" + word + "' is already in the word list.", NamedTextColor.RED));
                    return;
                }
                words.add(word);
                plugin.getConfig().set("chat-games.words", words);
                plugin.saveConfig();
                sender.sendMessage(Component.text("Added '" + word + "' to the chat game words.", NamedTextColor.GREEN));
            }

            case "remove" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /chatgame remove <word>", NamedTextColor.YELLOW)); return; }
                String word = args[1];
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                boolean removed = words.removeIf(w -> w.equalsIgnoreCase(word));
                if (!removed) { sender.sendMessage(Component.text("'" + word + "' is not in the word list.", NamedTextColor.RED)); return; }
                plugin.getConfig().set("chat-games.words", words);
                plugin.saveConfig();
                sender.sendMessage(Component.text("Removed '" + word + "' from the chat game words.", NamedTextColor.GREEN));
            }

            case "list" -> {
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                if (words.isEmpty()) {
                    sender.sendMessage(Component.text("No words configured. Add one with /chatgame add <word>.", NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text("Chat game words (" + words.size() + "):", NamedTextColor.GOLD));
                sender.sendMessage(TextUtil.parse("&e" + String.join("&7, &e", words)));
            }

            default -> sender.sendMessage(Component.text("Unknown subcommand. Use /chatgame help.", NamedTextColor.RED));
        }
    }
}

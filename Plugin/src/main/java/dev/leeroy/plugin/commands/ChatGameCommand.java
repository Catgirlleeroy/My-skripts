package dev.leeroy.plugin.commands;

import dev.leeroy.plugin.Utils.ChatGameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ChatGameCommand implements CommandExecutor {

    private final ChatGameManager chatGameManager;
    private final JavaPlugin plugin;

    public ChatGameCommand(ChatGameManager chatGameManager, JavaPlugin plugin) {
        this.chatGameManager = chatGameManager;
        this.plugin          = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("bob.chatgame")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "── Chat Game Commands ──");
            sender.sendMessage(ChatColor.YELLOW + "/chatgame now" + ChatColor.GRAY + " — Start a game immediately");
            sender.sendMessage(ChatColor.YELLOW + "/chatgame add <word>" + ChatColor.GRAY + " — Add a word");
            sender.sendMessage(ChatColor.YELLOW + "/chatgame remove <word>" + ChatColor.GRAY + " — Remove a word");
            sender.sendMessage(ChatColor.YELLOW + "/chatgame list" + ChatColor.GRAY + " — List all words");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "now" -> {
                if (!plugin.getConfig().getBoolean("chat-games.enabled", true)) {
                    sender.sendMessage(ChatColor.RED + "Chat games are currently disabled in config.yml.");
                    return true;
                }
                if (chatGameManager.isActive()) {
                    sender.sendMessage(ChatColor.RED + "A chat game is already running!");
                    return true;
                }
                chatGameManager.startGame();
                sender.sendMessage(ChatColor.GREEN + "Chat game started!");
            }

            case "add" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /chatgame add <word>"); return true; }
                String word = args[1];
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                if (words.stream().anyMatch(w -> w.equalsIgnoreCase(word))) {
                    sender.sendMessage(ChatColor.RED + "'" + word + "' is already in the word list.");
                    return true;
                }
                words.add(word);
                plugin.getConfig().set("chat-games.words", words);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Added '" + word + "' to the chat game words.");
            }

            case "remove" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /chatgame remove <word>"); return true; }
                String word = args[1];
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                boolean removed = words.removeIf(w -> w.equalsIgnoreCase(word));
                if (!removed) { sender.sendMessage(ChatColor.RED + "'" + word + "' is not in the word list."); return true; }
                plugin.getConfig().set("chat-games.words", words);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Removed '" + word + "' from the chat game words.");
            }

            case "list" -> {
                List<String> words = plugin.getConfig().getStringList("chat-games.words");
                if (words.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "No words configured. Add one with /chatgame add <word>.");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "Chat game words (" + words.size() + "):");
                sender.sendMessage(ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, words));
            }

            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /chatgame help.");
        }

        return true;
    }
}
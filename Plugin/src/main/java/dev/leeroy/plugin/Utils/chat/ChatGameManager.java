package dev.leeroy.plugin.Utils.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class ChatGameManager {

    private final JavaPlugin plugin;

    private String activeWord    = null;   // current reaction word, null if no game running
    private long   startTimeMs   = 0;      // when the current game started
    private BukkitTask timeoutTask = null; // task that fires when game expires
    private BukkitTask intervalTask = null;// recurring auto-start task

    public ChatGameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startInterval();
    }

    // ── Auto interval ─────────────────────────────────────────────────────────

    public void startInterval() {
        FileConfiguration cfg = plugin.getConfig();

        if (!cfg.getBoolean("chat-games.enabled", true)) return;

        int minutes = cfg.getInt("chat-games.interval", 5);
        long ticks  = minutes * 60L * 20L;

        intervalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::startGame, ticks, ticks);
    }

    public void restart() {
        if (intervalTask != null) intervalTask.cancel();
        if (timeoutTask  != null) timeoutTask.cancel();
        activeWord = null;
        startInterval();
    }

    // ── Start a game ──────────────────────────────────────────────────────────

    public void startGame() {
        FileConfiguration cfg = plugin.getConfig();
        List<String> words = cfg.getStringList("chat-games.words");

        if (words.isEmpty()) {
            plugin.getLogger().warning("[Bob] Chat games: no words configured!");
            return;
        }

        // Pick random word
        activeWord  = words.get(new Random().nextInt(words.size()));
        startTimeMs = System.currentTimeMillis();

        String prefix  = color(cfg.getString("chat-games.announce-prefix", ""));
        String startMsg = color(cfg.getString("chat-games.messages.start",
                        "&7&l| &cA chat game has started! Type &4{word} &cto gain rewards!")
                .replace("{word}", activeWord));

        broadcast(prefix, startMsg);
        playStartSound();

        // Schedule timeout
        int timeoutSecs = cfg.getInt("chat-games.timeout", 40);
        final String word = activeWord;
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeWord != null && activeWord.equals(word)) {
                String timeoutMsg = color(cfg.getString("chat-games.messages.timeout",
                                "&7&l| &fNo one was able to answer &4{word} &fin 40 seconds!")
                        .replace("{word}", word));
                broadcast(prefix, timeoutMsg);
                playTimeoutSound();
                activeWord = null;
            }
        }, timeoutSecs * 20L);
    }

    // ── Chat check ────────────────────────────────────────────────────────────

    /**
     * Called from the chat listener. Returns true if the message matches
     * the active word and the game should be cancelled.
     */
    public boolean handleChat(Player player, String message) {
        if (activeWord == null) return false;
        if (!message.trim().equalsIgnoreCase(activeWord.trim())) return false;

        FileConfiguration cfg = plugin.getConfig();

        // Calculate time taken
        long elapsed = System.currentTimeMillis() - startTimeMs;
        String timeTaken = formatElapsed(elapsed);

        String prefix  = color(cfg.getString("chat-games.announce-prefix", ""));
        String winMsg  = color(cfg.getString("chat-games.messages.win",
                        "&7&l| &4{player} &chas gotten &4{word} &cin &4{time}&c!")
                .replace("{player}", player.getName())
                .replace("{word}",   activeWord)
                .replace("{time}",   timeTaken));

        broadcast(prefix, winMsg);
        playStartSound();

        // Run rewards
        List<String> rewards = cfg.getStringList("chat-games.rewards");
        for (String cmd : rewards) {
            String finalCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        // Clean up
        if (timeoutTask != null) timeoutTask.cancel();
        activeWord = null;
        return true;
    }

    public boolean isActive() { return activeWord != null; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcast(String prefix, String message) {
        Bukkit.broadcastMessage("");
        if (!prefix.isEmpty()) Bukkit.broadcastMessage(prefix);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(message);
        Bukkit.broadcastMessage("");
    }

    private void playStartSound() {
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 1.0f)
        );
    }

    private void playTimeoutSound() {
        float[] pitches = {0.6f, 0.4f, 0.2f};
        for (int i = 0; i < pitches.length; i++) {
            final float pitch = pitches[i];
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    Bukkit.getOnlinePlayers().forEach(p ->
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, pitch)
                    ), i * 5L);
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String formatElapsed(long ms) {
        long seconds = ms / 1000;
        long millis  = ms % 1000;
        if (seconds == 0) return millis + "ms";
        return seconds + "." + String.format("%03d", millis) + "s";
    }
}
package dev.leeroy.plugin.Utils.chat;

import dev.leeroy.plugin.Utils.misc.TextUtil;
import org.bukkit.Bukkit;
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

        String prefix  = cfg.getString("chat-games.announce-prefix", "");
        String startMsg = cfg.getString("chat-games.messages.start",
                        "&7&l| &cA chat game has started! Type &4{word} &cto gain rewards!")
                .replace("{word}", activeWord);

        broadcast(prefix, startMsg);
        playStartSound();

        // Schedule timeout
        int timeoutSecs = cfg.getInt("chat-games.timeout", 40);
        final String word = activeWord;
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeWord != null && activeWord.equals(word)) {
                String timeoutMsg = cfg.getString("chat-games.messages.timeout",
                                "&7&l| &fNo one was able to answer &4{word} &fin 40 seconds!")
                        .replace("{word}", word);
                broadcast(prefix, timeoutMsg);
                playTimeoutSound();
                activeWord = null;
            }
        }, timeoutSecs * 20L);
    }

    // ── Chat check ────────────────────────────────────────────────────────────

    /** Thread-safe check: does the message match the active word? */
    public boolean isCorrectAnswer(String message) {
        return activeWord != null && message.trim().equalsIgnoreCase(activeWord.trim());
    }

    /** Called on the main thread after a correct answer is confirmed. */
    public void handleWin(Player player, String message) {
        if (activeWord == null) return;
        if (!message.trim().equalsIgnoreCase(activeWord.trim())) return;

        FileConfiguration cfg = plugin.getConfig();
        long elapsed = System.currentTimeMillis() - startTimeMs;
        String timeTaken = formatElapsed(elapsed);

        String prefix = cfg.getString("chat-games.announce-prefix", "");
        String winMsg = cfg.getString("chat-games.messages.win",
                        "&7&l| &4{player} &chas gotten &4{word} &cin &4{time}&c!")
                .replace("{player}", player.getName())
                .replace("{word}",   activeWord)
                .replace("{time}",   timeTaken);

        broadcast(prefix, winMsg);
        playStartSound();

        List<String> rewards = cfg.getStringList("chat-games.rewards");
        for (String cmd : rewards) {
            if (player.getName().matches("[a-zA-Z0-9_]{1,16}")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
            }
        }

        if (timeoutTask != null) timeoutTask.cancel();
        activeWord = null;
    }

    public boolean isActive() { return activeWord != null; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcast(String prefix, String message) {
        // Send directly to each player to avoid DiscordSRV picking up the messages
        java.util.function.Consumer<org.bukkit.command.CommandSender> send = sender -> {
            sender.sendMessage(TextUtil.parse(""));
            if (!prefix.isEmpty()) sender.sendMessage(TextUtil.parse(prefix));
            sender.sendMessage(TextUtil.parse(""));
            sender.sendMessage(TextUtil.parse(message));
            sender.sendMessage(TextUtil.parse(""));
        };
        Bukkit.getOnlinePlayers().forEach(send);
        send.accept(Bukkit.getConsoleSender());
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

    private String formatElapsed(long ms) {
        long seconds = ms / 1000;
        long millis  = ms % 1000;
        if (seconds == 0) return millis + "ms";
        return seconds + "." + String.format("%03d", millis) + "s";
    }
}
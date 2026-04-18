package dev.leeroy.plugin.commands.misc;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Gamemodes implements BasicCommand {

    private final GameMode mode;
    private final String cmdLabel;

    public Gamemodes(GameMode mode, String cmdLabel) {
        this.mode     = mode;
        this.cmdLabel = cmdLabel;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length > 1) return java.util.Collections.emptyList();
        String lower = (args.length == 1 ? args[0] : "").toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(lower)) result.add(p.getName());
        }
        return result;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a player: /" + cmdLabel + " <player>", NamedTextColor.RED));
                return;
            }

            String selfPerm = "bob.gamemode.self." + mode.name().toLowerCase();

            if (!player.hasPermission(selfPerm) && !player.hasPermission("bob.gamemode.self")) {
                player.sendMessage(Component.text("You don't have permission to change your gamemode to " + formatGameMode(mode) + ".", NamedTextColor.RED));
                return;
            }

            player.setGameMode(mode);
            player.sendMessage(Component.text("Your gamemode has been set to " + formatGameMode(mode) + ".", NamedTextColor.GREEN));
            return;
        }

        if (args.length == 1) {
            String othersPerm = "bob.gamemode.others." + mode.name().toLowerCase();

            if (!sender.hasPermission(othersPerm) && !sender.hasPermission("bob.gamemode.others")) {
                sender.sendMessage(Component.text("You don't have permission to change other players' gamemode to " + formatGameMode(mode) + ".", NamedTextColor.RED));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player '" + args[0] + "' not found or is offline.", NamedTextColor.RED));
                return;
            }

            target.setGameMode(mode);
            target.sendMessage(Component.text("Your gamemode has been set to " + formatGameMode(mode) + " by " + sender.getName() + ".", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Set " + target.getName() + "'s gamemode to " + formatGameMode(mode) + ".", NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("Usage: /" + cmdLabel + " [player]", NamedTextColor.YELLOW));
    }

    private String formatGameMode(GameMode gm) {
        return switch (gm) {
            case SURVIVAL  -> "Survival";
            case CREATIVE  -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }
}

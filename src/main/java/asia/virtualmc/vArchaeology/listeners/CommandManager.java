package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandManager implements CommandExecutor {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> resetConfirmations;
    private static final long RESET_TIMEOUT = 10000;

    public CommandManager(Main plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.resetConfirmations = new HashMap<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("varch")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "get" -> handleGet(sender, args);
            case "exp", "level" -> handleStatModification(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /varch get <player>")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(Component.text("No data found for that player!")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        sender.sendMessage(Component.text("=== Player Data for " + target.getName() + " ===")
                .color(TextColor.color(0, 255, 162)));
        sender.sendMessage(Component.text("Archaeology EXP: " + data.getArchExp())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("Archaeology Level: " + data.getArchLevel())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("Aptitude: " + data.getArchApt())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("Luck: " + data.getArchLuck())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("ADP: " + data.getArchADP())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("XP Multiplier: " + data.getArchXPMul())
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("Bonus XP: " + data.getArchBonusXP())
                .color(TextColor.color(255, 255, 255)));
    }

    private void handleStatModification(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(Component.text("Usage: /varch <exp/level> <add/subtract/set> <player> <value>")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        String type = args[0].toLowerCase();
        String operation = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);
        int value;

        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number format!")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        if (type.equals("exp")) {
            playerDataManager.updateExp(target.getUniqueId(), value, operation);
        } else {
            playerDataManager.updateLevel(target.getUniqueId(), value, operation);
        }

        sender.sendMessage(Component.text("Successfully updated " + target.getName() + "'s " + type + "!")
                .color(TextColor.color(0, 255, 162)));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /varch reset <player> [confirm]")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(TextColor.color(255, 0, 0)));
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (args.length == 2) {
            // First confirmation
            resetConfirmations.put(targetUUID, System.currentTimeMillis());
            sender.sendMessage(Component.text("Are you sure you want to reset " + target.getName() + "'s data?")
                    .color(TextColor.color(255, 255, 0)));
            sender.sendMessage(Component.text("Type '/varch reset " + target.getName() + " confirm' within 10 seconds to confirm.")
                    .color(TextColor.color(255, 255, 0)));
            return;
        }

        if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
            Long confirmationTime = resetConfirmations.get(targetUUID);
            if (confirmationTime == null || System.currentTimeMillis() - confirmationTime > RESET_TIMEOUT) {
                sender.sendMessage(Component.text("Reset confirmation has expired. Please try again.")
                        .color(TextColor.color(255, 0, 0)));
                resetConfirmations.remove(targetUUID);
                return;
            }

            // Reset the player's data
            PlayerData newData = new PlayerData(playerDataManager);
            playerDataManager.unloadData(targetUUID);
            playerDataManager.loadData(targetUUID);

            sender.sendMessage(Component.text("Successfully reset " + target.getName() + "'s data!")
                    .color(TextColor.color(0, 255, 162)));
            resetConfirmations.remove(targetUUID);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== vArchaeology Commands ===")
                .color(TextColor.color(0, 255, 162)));
        sender.sendMessage(Component.text("/varch get <player> - View player's archaeology data")
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("/varch <exp/level> <add/subtract/set> <player> <value> - Modify player's stats")
                .color(TextColor.color(255, 255, 255)));
        sender.sendMessage(Component.text("/varch reset <player> - Reset player's data")
                .color(TextColor.color(255, 255, 255)));
    }
}

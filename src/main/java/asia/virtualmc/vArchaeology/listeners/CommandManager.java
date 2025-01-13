package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandManager {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> resetConfirmations;
    private static final long RESET_TIMEOUT = 10000;

    public CommandManager(Main plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.resetConfirmations = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archGetStats())
                .withSubcommand(archSetEXP())
                .withSubcommand(archSetLevel())
                .withSubcommand(archResetStats())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archGetStats() {
        return new CommandAPICommand("get")
                .withArguments(new PlayerArgument("player"))
                .withPermission("varchaeology.command.get")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    PlayerData data = playerDataManager.getPlayerData(target.getUniqueId());

                    if (data == null) {
                        sender.sendMessage(Component.text("[vArchaeology] No data found for that player!")
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
                });
    }

    private CommandAPICommand archSetEXP() {
        return new CommandAPICommand("exp")
                .withArguments(new MultiLiteralArgument("operation", "add", "subtract", "set"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 0))
                .withPermission("varchaeology.command.exp")
                .executes((sender, args) -> {
                    String operation = (String) args.get("operation");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    playerDataManager.updateExp(target.getUniqueId(), value, operation);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully updated " + target.getName() + "'s exp!")
                            .color(TextColor.color(0, 255, 162)));
                });
    }

    private CommandAPICommand archSetLevel() {
        return new CommandAPICommand("level")
                .withArguments(new MultiLiteralArgument("operation", "add", "subtract", "set"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 0))
                .withPermission("varchaeology.command.level")
                .executes((sender, args) -> {
                    String operation = (String) args.get("operation");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    playerDataManager.updateLevel(target.getUniqueId(), value, operation);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully updated " + target.getName() + "'s level!")
                            .color(TextColor.color(0, 255, 162)));
                });
    }

    private CommandAPICommand archResetStats() {
        return new CommandAPICommand("reset")
                .withArguments(new PlayerArgument("player"))
                .withOptionalArguments(new LiteralArgument("confirm"))
                .withPermission("varchaeology.command.reset")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    UUID targetUUID = target.getUniqueId();
                    boolean confirm = args.get("confirm") != null;

                    if (!confirm) {
                        resetConfirmations.put(targetUUID, System.currentTimeMillis());
                        sender.sendMessage(Component.text("[vArchaeology] Are you sure you want to reset " + target.getName() + "'s data?")
                                .color(TextColor.color(255, 255, 0)));
                        sender.sendMessage(Component.text("[vArchaeology] Type '/varch reset " + target.getName() + " confirm' within 10 seconds to confirm.")
                                .color(TextColor.color(255, 255, 0)));
                        return;
                    }

                    Long confirmationTime = resetConfirmations.get(targetUUID);
                    if (confirmationTime == null || System.currentTimeMillis() - confirmationTime > RESET_TIMEOUT) {
                        sender.sendMessage(Component.text("[vArchaeology] Reset confirmation has expired. Please try again.")
                                .color(TextColor.color(255, 0, 0)));
                        resetConfirmations.remove(targetUUID);
                        return;
                    }

                    playerDataManager.unloadData(targetUUID);
                    playerDataManager.loadData(targetUUID);

                    sender.sendMessage(Component.text("[vArchaeology] Successfully reset " + target.getName() + "'s data!")
                            .color(TextColor.color(0, 255, 162)));
                    resetConfirmations.remove(targetUUID);
                });
    }
}
package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.guis.*;

import dev.jorel.commandapi.CommandAPICommand;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUICommands {
    private final Main plugin;
    private final SellGUI sellGUI;
    private final SalvageGUI salvageGUI;
    private final TraitGUI traitGUI;
    private final ArtefactRestorationGUI artefactRestorationGUI;
    private final RankGUI rankGUI;
    private final Map<UUID, Long> commandCooldowns;

    public GUICommands(Main plugin,
                       SellGUI sellGUI,
                       SalvageGUI salvageGUI,
                       TraitGUI traitGUI,
                       ArtefactRestorationGUI artefactRestorationGUI,
                       RankGUI rankGUI) {
        this.plugin = plugin;
        this.sellGUI = sellGUI;
        this.salvageGUI = salvageGUI;
        this.traitGUI = traitGUI;
        this.artefactRestorationGUI = artefactRestorationGUI;
        this.rankGUI = rankGUI;
        this.commandCooldowns = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archSellGUI())
                .withSubcommand(archSalvageGUI())
                .withSubcommand(archComponentsGUI())
                .withSubcommand(archTraitGUI())
                .withSubcommand(archRestoreArtefact())
                .withSubcommand(archRankGUI())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archSellGUI() {
        return new CommandAPICommand("sell")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        UUID playerUUID = player.getUniqueId();
                        long currentTime = System.currentTimeMillis();

                        if (commandCooldowns.containsKey(playerUUID)) {
                            long lastUsageTime = commandCooldowns.get(playerUUID);
                            if (currentTime - lastUsageTime < 2000) { // 1000 ms = 1 second
                                sender.sendMessage(Component.text("You can only use this command every 2 seconds.")
                                        .color(TextColor.color(255, 0, 0)));
                                return;
                            }
                        }
                        commandCooldowns.put(playerUUID, currentTime);
                        sellGUI.openSellGUI(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archSalvageGUI() {
        return new CommandAPICommand("salvage")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        UUID playerUUID = player.getUniqueId();
                        long currentTime = System.currentTimeMillis();

                        if (commandCooldowns.containsKey(playerUUID)) {
                            long lastUsageTime = commandCooldowns.get(playerUUID);
                            if (currentTime - lastUsageTime < 2000) { // 1000 ms = 1 second
                                sender.sendMessage(Component.text("You can only use this command every 2 seconds.")
                                        .color(TextColor.color(255, 0, 0)));
                                return;
                            }
                        }
                        commandCooldowns.put(playerUUID, currentTime);
                        salvageGUI.openSalvageGUI(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archComponentsGUI() {
        return new CommandAPICommand("comp")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        salvageGUI.openComponentsGUI(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archTraitGUI() {
        return new CommandAPICommand("trait")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        traitGUI.openInfoMode(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archRestoreArtefact() {
        return new CommandAPICommand("restore")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        artefactRestorationGUI.openRestoreArtefact(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archRankGUI() {
        return new CommandAPICommand("rank")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        rankGUI.openRankGUI(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }
}
package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.blocks.CraftingStation;
import asia.virtualmc.vArchaeology.blocks.RestorationStation;
import asia.virtualmc.vArchaeology.items.CustomCharms;
import asia.virtualmc.vArchaeology.items.CustomItems;
import asia.virtualmc.vArchaeology.items.CustomTools;
import asia.virtualmc.vArchaeology.items.MiscItems;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.entity.Player;

public class BlockCommands {
    private final Main plugin;
    private final RestorationStation restorationStation;
    private final CraftingStation craftingStation;

    public BlockCommands(Main plugin,
                        RestorationStation restorationStation,
                         CraftingStation craftingStation) {
        this.plugin = plugin;
        this.restorationStation = restorationStation;
        this.craftingStation = craftingStation;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archCreateRestorationStation())
                .withSubcommand(archCreateCraftingStation())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archCreateRestorationStation() {
        return new CommandAPICommand("restorestation")
                .withArguments(new MultiLiteralArgument("operation", "create", "delete"))
                .withPermission("varchaeology.admin.salvage_station")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        restorationStation.createRestorationStation(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }

    private CommandAPICommand archCreateCraftingStation() {
        return new CommandAPICommand("craftstation")
                .withArguments(new MultiLiteralArgument("operation", "create", "delete"))
                .withPermission("varchaeology.admin.salvage_station")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        craftingStation.createCraftingStation(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }
}
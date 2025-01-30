package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;

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

    public BlockCommands(Main plugin,
                        RestorationStation restorationStation) {
        this.plugin = plugin;
        this.restorationStation = restorationStation;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archCreateSalvageStation())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archCreateSalvageStation() {
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
}
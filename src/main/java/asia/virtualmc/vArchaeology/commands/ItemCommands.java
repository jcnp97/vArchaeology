package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.items.ItemManager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.entity.Player;

public class ItemCommands {
    private final Main plugin;
    private final ItemManager itemManager;

    public ItemCommands(Main plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archGetItem())
                .withSubcommand(archGetTool())
                .withSubcommand(archGetCharm())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archGetItem() {
        return new CommandAPICommand("getitem")
                .withArguments(new MultiLiteralArgument("item_name", "purpleheart_wood", "imperial_steel", "everlight_silvthril", "chaotic_brimstone", "hellfire_metal", "aetherium_alloy", "quintessence"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 1))
                .withPermission("varchaeology.command.getitem")
                .executes((sender, args) -> {
                    String itemName = (String) args.get("item_name");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    int itemId = getItemIdFromName(itemName);
                    sender.sendMessage("Attempting to give " + value + " of " + itemName + " to " + target.getName());
                    itemManager.giveArchItem(target.getUniqueId(), itemId, value);
                });
    }

    private int getItemIdFromName(String name) {
        return switch (name.toLowerCase()) {
            case "purpleheart_wood" -> 1;
            case "imperial_steel" -> 2;
            case "everlight_silvthril" -> 3;
            case "chaotic_brimstone" -> 4;
            case "hellfire_metal" -> 5;
            case "aetherium_alloy" -> 6;
            case "quintessence" -> 7;
            default -> throw new IllegalArgumentException("Unknown item: " + name);
        };
    }

    private CommandAPICommand archGetTool() {
        return new CommandAPICommand("gettool")
                .withArguments(new MultiLiteralArgument("tool_name", "bronze_mattock", "iron_mattock",
                        "steel_mattock", "mithril_mattock", "adamantium_mattock", "runite_mattock", "dragon_mattock",
                        "necronium_mattock", "crystal_mattock", "mattock_of_time_and_space"
                ))
                .withArguments(new PlayerArgument("player"))
                .withPermission("varchaeology.command.gettool")
                .executes((sender, args) -> {
                    String toolName = (String) args.get("tool_name");
                    Player target = (Player) args.get("player");

                    int itemId = getToolIdFromName(toolName);
                    sender.sendMessage("Attempting to give " + toolName + " to " + target.getName());
                    itemManager.giveArchTool(target.getUniqueId(), itemId);
                });
    }

    private int getToolIdFromName(String name) {
        return switch (name.toLowerCase()) {
            case "bronze_mattock" -> 1;
            case "iron_mattock" -> 2;
            case "steel_mattock" -> 3;
            case "mithril_mattock" -> 4;
            case "adamantium_mattock" -> 5;
            case "runite_mattock" -> 6;
            case "dragon_mattock" -> 7;
            case "necronium_mattock" -> 8;
            case "crystal_mattock" -> 9;
            case "mattock_of_time_and_space" -> 10;
            default -> throw new IllegalArgumentException("Unknown item: " + name);
        };
    }

    private CommandAPICommand archGetCharm() {
        return new CommandAPICommand("getcharm")
                .withArguments(new MultiLiteralArgument("item_name", "common", "uncommon", "rare", "unique", "special", "mythical", "exotic"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 1))
                .withPermission("varchaeology.command.getcharm")
                .executes((sender, args) -> {
                    String itemName = (String) args.get("item_name");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    int itemId = getCharmIdFromName(itemName);
                    sender.sendMessage("Attempting to give " + value + " of " + itemName + " to " + target.getName());
                    itemManager.giveArchCharm(target.getUniqueId(), itemId, value);
                });
    }

    private int getCharmIdFromName(String name) {
        return switch (name.toLowerCase()) {
            case "common" -> 1;
            case "uncommon" -> 2;
            case "rare" -> 3;
            case "unique" -> 4;
            case "special" -> 5;
            case "mythical" -> 6;
            case "exotic" -> 7;
            default -> throw new IllegalArgumentException("Unknown item: " + name);
        };
    }
}
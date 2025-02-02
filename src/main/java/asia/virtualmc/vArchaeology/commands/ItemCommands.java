package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.items.*;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.entity.Player;

public class ItemCommands {
    private final Main plugin;
    private final CustomTools customTools;
    private final CustomCharms customCharms;
    private final CustomItems customItems;
    private final MiscItems miscItems;
    private final ArtefactCollections artefactCollections;

    public ItemCommands(Main plugin,
                        CustomItems customItems,
                        CustomTools customTools,
                        CustomCharms customCharms,
                        MiscItems miscItems,
                        ArtefactCollections artefactCollections) {
        this.plugin = plugin;
        this.customItems = customItems;
        this.customTools = customTools;
        this.customCharms = customCharms;
        this.miscItems = miscItems;
        this.artefactCollections = artefactCollections;
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archGetItem())
                .withSubcommand(archGetTool())
                .withSubcommand(archGetCharm())
                .withSubcommand(archGetLamps())
                .withSubcommand(archGetStars())
                .withSubcommand(archGetCollection())
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
                    customItems.giveArchItem(target.getUniqueId(), itemId, value);
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
                .withArguments(new MultiLiteralArgument("tool_name", "copper_mattock", "flint_mattock",
                        "prismarine_mattock", "carbon_steel_mattock", "netherium_mattock", "amethyst_mattock", "gold_alloy_mattock",
                        "titanium_mattock", "dark_echo_mattock", "mattock_of_time_and_space", "admin"
                ))
                .withArguments(new PlayerArgument("player"))
                .withPermission("varchaeology.command.gettool")
                .executes((sender, args) -> {
                    String toolName = (String) args.get("tool_name");
                    Player target = (Player) args.get("player");

                    int itemId = getToolIdFromName(toolName);
                    sender.sendMessage("Attempting to give " + toolName + " to " + target.getName());
                    customTools.giveArchTool(target.getUniqueId(), itemId);
                });
    }

    private int getToolIdFromName(String name) {
        return switch (name.toLowerCase()) {
            case "copper_mattock" -> 1;
            case "flint_mattock" -> 2;
            case "prismarine_mattock" -> 3;
            case "carbon_steel_mattock" -> 4;
            case "netherium_mattock" -> 5;
            case "amethyst_mattock" -> 6;
            case "gold_alloy_mattock" -> 7;
            case "titanium_mattock" -> 8;
            case "dark_echo_mattock" -> 9;
            case "mattock_of_time_and_space" -> 10;
            case "admin" -> 11;
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
                    customCharms.giveCharm(target.getUniqueId(), itemId, value);
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

    private CommandAPICommand archGetLamps() {
        return new CommandAPICommand("getlamp")
                .withArguments(new MultiLiteralArgument("item_name", "small", "medium", "large", "huge"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 1))
                .withPermission("varchaeology.command.getlamp")
                .executes((sender, args) -> {
                    String itemName = (String) args.get("item_name");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    int itemId = getLampIDFromName(itemName);
                    sender.sendMessage("Attempting to give " + value + " of " + itemName + " to " + target.getName());
                    miscItems.giveLamp(target.getUniqueId(), itemId, value);
                });
    }

    private int getLampIDFromName(String name) {
        return switch (name.toLowerCase()) {
            case "small" -> 1;
            case "medium" -> 2;
            case "large" -> 3;
            case "huge" -> 4;
            default -> throw new IllegalArgumentException("Unknown item: " + name);
        };
    }

    private CommandAPICommand archGetStars() {
        return new CommandAPICommand("getstar")
                .withArguments(new MultiLiteralArgument("item_name", "small", "medium", "large", "huge"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 1))
                .withPermission("varchaeology.command.getstar")
                .executes((sender, args) -> {
                    String itemName = (String) args.get("item_name");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    int itemId = getStarIDFromName(itemName);
                    sender.sendMessage("Attempting to give " + value + " of " + itemName + " to " + target.getName());
                    miscItems.giveStar(target.getUniqueId(), itemId, value);
                });
    }

    private int getStarIDFromName(String name) {
        return switch (name.toLowerCase()) {
            case "small" -> 1;
            case "medium" -> 2;
            case "large" -> 3;
            case "huge" -> 4;
            default -> throw new IllegalArgumentException("Unknown item: " + name);
        };
    }

    private CommandAPICommand archGetCollection() {
        return new CommandAPICommand("getcollection")
                .withArguments(new IntegerArgument("item_id", 1))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 1))
                .withPermission("varchaeology.command.getcollection")
                .executes((sender, args) -> {
                    int itemID = (int) args.get("item_id");
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    sender.sendMessage("Attempting to give " + value + " of " + itemID + " to " + target.getName());
                    artefactCollections.giveCollection(target.getUniqueId(), itemID, value);
                });
    }
}
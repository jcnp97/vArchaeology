package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;
import asia.virtualmc.vArchaeology.guis.SellGUIManager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandManager {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;
    private final TalentTreeManager talentTreeManager;
    private final StatsManager statsManager;
    private final SellGUIManager sellGUIManager;
    private final Map<UUID, Long> resetConfirmations;
    private static final long RESET_TIMEOUT = 10000;

    public CommandManager(Main plugin, PlayerDataManager playerDataManager, ItemManager itemManager, TalentTreeManager talentTreeManager, StatsManager statsManager, SellGUIManager sellGUIManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
        this.statsManager = statsManager;
        this.talentTreeManager = talentTreeManager;
        this.sellGUIManager = sellGUIManager;
        this.resetConfirmations = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        new CommandAPICommand("varch")
                .withSubcommand(archGetStats())
                .withSubcommand(archSetEXP())
                .withSubcommand(archSetLevel())
                .withSubcommand(archSetXPMul())
                .withSubcommand(archResetStats())
                .withSubcommand(archGetItem())
                .withSubcommand(archGetTool())
                .withSubcommand(archSetTalent())
                .withSubcommand(archAddBonusXP())
                .withSubcommand(archSellGUI())
                .withHelp("[vArchaeology] Main command for vArchaeology", "Access vArchaeology commands")
                .register();
    }

    private CommandAPICommand archGetStats() {
        return new CommandAPICommand("get")
                .withArguments(new PlayerArgument("player"))
                .withPermission("varchaeology.command.get")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    UUID targetUUID = target.getUniqueId();
                    String playerName = playerDataManager.getPlayerName(targetUUID);
                    String archEXP = String.format("%,.2f", playerDataManager.getArchExp(targetUUID));

                    if (playerName == null) {
                        sender.sendMessage(Component.text("[vArchaeology] No data found for that player!")
                                .color(TextColor.color(255, 0, 0)));
                        return;
                    }

                    sender.sendMessage(Component.text("=== Player Data for " + target.getName() + " ===")
                            .color(TextColor.color(0, 255, 162)));
                    sender.sendMessage(Component.text("Archaeology EXP: " + archEXP)
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Archaeology Level: " + playerDataManager.getArchLevel(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Aptitude: " + playerDataManager.getArchApt(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Luck: " + playerDataManager.getArchLuck(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("ADP: " + playerDataManager.getArchADP(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("XP Multiplier: " + playerDataManager.getArchXPMul(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Bonus XP: " + playerDataManager.getArchBonusXP(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Blocks Mined: " + statsManager.getStatistics(targetUUID, 8))
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

    private CommandAPICommand archSetXPMul() {
        return new CommandAPICommand("xpmul")
                .withArguments(new MultiLiteralArgument("operation", "add", "subtract", "set"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new DoubleArgument("value", 0.0))
                .withPermission("varchaeology.command.xpmul")
                .executes((sender, args) -> {
                    String operation = (String) args.get("operation");
                    Player target = (Player) args.get("player");
                    double value = (double) args.get("value");

                    playerDataManager.updateXPMul(target.getUniqueId(), value, operation);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully updated " + target.getName() + "'s XP Multiplier!")
                            .color(TextColor.color(0, 255, 162)));
                });
    }

    private CommandAPICommand archAddBonusXP() {
        return new CommandAPICommand("bonusxp")
                .withArguments(new MultiLiteralArgument("operation", "add"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 0))
                .withPermission("varchaeology.command.bonusxp")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    int value = (int) args.get("value");

                    playerDataManager.addBonusXP(target.getUniqueId(), value);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully updated " + target.getName() + "'s Bonus XP!")
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

    // Item Commands
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

    private CommandAPICommand archSetTalent() {
        return new CommandAPICommand("talent")
                .withArguments(new MultiLiteralArgument("operation", "set"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("talentID", 0))
                .withArguments(new IntegerArgument("level", 0))
                .withPermission("varchaeology.command.settalent")
                .executes((sender, args) -> {
                    String operation = (String) args.get("operation");
                    Player target = (Player) args.get("player");
                    int talentID = (int) args.get("talentID");
                    int level = (int) args.get("level");

                    talentTreeManager.updateTalentLevel(target.getUniqueId(), talentID, level);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully set " + target.getName() + "'s Talent " + talentID + " to level " + level)
                            .color(TextColor.color(0, 255, 162)));
                });
    }

    private CommandAPICommand archSellGUI() {
        return new CommandAPICommand("sell")
                .withPermission("varchaeology.use")
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        sellGUIManager.openSellGUI(player);
                    } else {
                        sender.sendMessage("This command can only be used by players.");
                    }
                });
    }
}
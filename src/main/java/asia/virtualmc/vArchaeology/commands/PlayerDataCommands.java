package asia.virtualmc.vArchaeology.commands;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.guis.RankGUI;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.TalentTree;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataCommands {
    private final Main plugin;
    private final PlayerData playerData;
    private final TalentTree talentTree;
    private final RankGUI rankGUI;
    private final Map<UUID, Long> resetConfirmations;
    private static final long RESET_TIMEOUT = 10000;

    public PlayerDataCommands(Main plugin,
                              PlayerData playerData,
                              TalentTree talentTree,
                              RankGUI rankGUI) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.talentTree = talentTree;
        this.rankGUI = rankGUI;
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
                .withSubcommand(archAddBonusXP())
                .withSubcommand(archSetRankPoints())
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
                    String playerName = playerData.getPlayerName(targetUUID);
                    String archEXP = String.format("%,.2f", playerData.getArchExp(targetUUID));

                    if (playerName == null) {
                        sender.sendMessage(Component.text("[vArchaeology] No data found for that player!")
                                .color(TextColor.color(255, 0, 0)));
                        return;
                    }
                    sender.sendMessage(Component.text("=== Player Data for " + target.getName() + " ===")
                            .color(TextColor.color(0, 255, 162)));
                    sender.sendMessage(Component.text("Archaeology EXP: " + archEXP)
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Archaeology Level: " + playerData.getArchLevel(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Aptitude: " + playerData.getArchApt(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Luck: " + playerData.getArchLuck(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("ADP: " + playerData.getArchADP(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("XP Multiplier: " + playerData.getArchXPMul(targetUUID))
                            .color(TextColor.color(255, 255, 255)));
                    sender.sendMessage(Component.text("Bonus XP: " + playerData.getArchBonusXP(targetUUID))
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

                    playerData.updateExp(target.getUniqueId(), value, operation);
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

                    playerData.updateLevel(target.getUniqueId(), value, operation);
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

                    playerData.updateXPMul(target.getUniqueId(), value, operation);
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

                    playerData.addBonusXP(target.getUniqueId(), value);
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

                    playerData.unloadData(targetUUID);
                    playerData.loadData(targetUUID);

                    sender.sendMessage(Component.text("[vArchaeology] Successfully reset " + target.getName() + "'s data!")
                            .color(TextColor.color(0, 255, 162)));
                    resetConfirmations.remove(targetUUID);
                });
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

                    talentTree.updateTalentLevel(target.getUniqueId(), talentID, level);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully set " + target.getName() + "'s Talent " + talentID + " to level " + level)
                            .color(TextColor.color(0, 255, 162)));
                });
    }

    private CommandAPICommand archSetRankPoints() {
        return new CommandAPICommand("rankpoints")
                .withArguments(new MultiLiteralArgument("operation", "set"))
                .withArguments(new PlayerArgument("player"))
                .withArguments(new IntegerArgument("value", 0))
                .withPermission("varchaeology.command.setrankpoints")
                .executes((sender, args) -> {
                    String operation = (String) args.get("operation");
                    Player target = (Player) args.get("player");
                    UUID targetUUID = target.getUniqueId();
                    int value = (int) args.get("value");

                    rankGUI.setRankPoints(targetUUID, value);
                    sender.sendMessage(Component.text("[vArchaeology] Successfully set " + target.getName() + "'s Rank Points " + " to " + value)
                            .color(TextColor.color(0, 255, 162)));
                });
    }
}
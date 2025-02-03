package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.TalentTree;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TalentGUI {
    private final Main plugin;
    private final TalentTree talentTree;
    private final PlayerData playerData;
    private final EffectsUtil effectsUtil;
    private final ConfigManager configManager;
    private final Map<Integer, ConfigManager.Talent> talents;

    public TalentGUI(Main plugin,
                     TalentTree talentTree,
                     PlayerData playerData,
                     EffectsUtil effectsUtil,
                     ConfigManager configManager
                     ) {
        this.plugin = plugin;
        this.talentTree = talentTree;
        this.playerData = playerData;
        this.effectsUtil = effectsUtil;
        this.configManager = configManager;
        this.talents = configManager.talentMap;
    }

    public void openTalentGUI(Player player) {
        ChestGui talentGui = new ChestGui(6, configManager.talentTreeTitle);
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        talentGui.setOnGlobalClick(event -> event.setCancelled(true));

        UUID uuid = player.getUniqueId();
        Map<Integer, Integer> playerTalents = talentTree.getPlayerTalentMap(uuid);
        int archLevel = playerData.getArchLevel(uuid);
        int talentID19 = playerTalents.getOrDefault(19, 0);
        int talentPoints = playerData.getTalentPoints(uuid);

        for (int i = 1; i <= 9; i++) {
            ConfigManager.Talent talent = talents.get(i);
            int talentLevel = playerTalents.getOrDefault(i, 0);
            ItemStack talentIcon = createTalentIcon(archLevel, talent, talentLevel);
            GuiItem talentGUI;

            if (archLevel >= talent.requiredLevel) {
                int talentID = i;
                talentGUI = new GuiItem(talentIcon, event -> {
                    if (talentPoints > 0 && talentLevel < (talentID19 * 10) + 10) {
                        openConfirmGUI(player, talentID);
                    } else if (talentPoints > 0) {
                        player.sendMessage("§cYou already reached the max level!");
                    } else {
                        player.sendMessage("§cYou don't have talent points!");
                    }
                });
            } else {
                talentGUI = new GuiItem(talentIcon);
            }

            pane.addItem(talentGUI, i - 1, 0);
        }

        for (int i = 10; i <= 19; i++) {
            ConfigManager.Talent talent = talents.get(i);
            int talentLevel = playerTalents.getOrDefault(i, 0);
            List<Integer> reqID = talent.requiredIDs;

            ItemStack talentIcon = createTalentIconSP(archLevel, talent, talentLevel, playerTalents, reqID);
            GuiItem talentGUI;

            boolean hasPrerequisiteTalent = hasPrerequisites(reqID, playerTalents);

            if (archLevel >= talent.requiredLevel && hasPrerequisiteTalent) {
                int talentID = i;
                talentGUI = new GuiItem(talentIcon, event -> {
                    if (talentPoints > 9 && talentLevel == 0) {
                        openConfirmGUI(player, talentID);
                    } else if (talentPoints > 9) {
                        player.sendMessage("§cYou already reached the max level!");
                    } else {
                        player.sendMessage("§cYou don't have enough talent points!");
                    }
                });
            } else {
                talentGUI = new GuiItem(talentIcon);
            }

            if (i == 19) {
                pane.addItem(talentGUI, 4, 2);
            } else {
                pane.addItem(talentGUI, i - 10, 1);
            }
        }

        talentGui.addPane(pane);
        talentGui.show(player);
    }

    private ItemStack createTalentIcon(int archLevel, ConfigManager.Talent talent, int talentLevel) {
        ItemStack talentIcon;
        List<String> lore = new ArrayList<>();
        boolean hasRequiredLevel = archLevel >= talent.requiredLevel;


        if (talentLevel > 0 && hasRequiredLevel) {
            talentIcon = new ItemStack(talent.material);
        } else {
            talentIcon = new ItemStack(Material.PAPER);
        }

        talentIcon.setAmount(talentLevel > 0 ? talentLevel : 1);
        ItemMeta meta = talentIcon.getItemMeta();

        if (talentLevel > 0 && hasRequiredLevel) {
            meta.setDisplayName("§6" + talent.name + " " + talentLevel + " §7(§a" +
                    (talent.loreData * talentLevel) + "%§7)");
            meta.setCustomModelData(talent.customModelData);
            meta.setLore(talent.lore);
        } else if (talentLevel == 0 && hasRequiredLevel) {
            meta.setDisplayName("§6" + talent.name);
            meta.setCustomModelData(100021);
            meta.setLore(talent.lore);
        } else {
            meta.setDisplayName("§6" + talent.name + " §c(Locked)");
            lore.add("§7Requires:");
            lore.add("§7• §c§mArchaeology Level " + talent.requiredLevel);
            meta.setLore(lore);
            meta.setCustomModelData(100020);
        }

        talentIcon.setItemMeta(meta);
        return talentIcon;
    }

    private ItemStack createTalentIconSP(int archLevel,
                                         ConfigManager.Talent talent,
                                         int talentLevel,
                                         Map<Integer, Integer> talentTree,
                                         List<Integer> requiredID) {
        ItemStack talentIcon;
        List<String> lore = new ArrayList<>();
        boolean hasPrerequisiteTalent = hasPrerequisites(requiredID, talentTree);
        boolean hasRequiredLevel = archLevel >= talent.requiredLevel;

        if (talentLevel > 0 && hasRequiredLevel) {
            talentIcon = new ItemStack(talent.material);
        } else {
            talentIcon = new ItemStack(Material.PAPER);
        }

        talentIcon.setAmount(talentLevel > 0 ? talentLevel : 1);
        ItemMeta meta = talentIcon.getItemMeta();

        if (hasRequiredLevel && hasPrerequisiteTalent) {
            meta.setDisplayName("§4" + talent.name);
            meta.setLore(talent.lore);

            if (talentLevel == 0) {
                meta.setCustomModelData(100021);
            } else {
                meta.setCustomModelData(talent.customModelData);
            }
        } else {
            meta.setDisplayName("§4" + talent.name + " §c(Locked)");
            meta.setCustomModelData(100020);
            lore.add("§7Requires:");
            for (int reqID : requiredID) {
                if (talentTree.getOrDefault(reqID, 0) < 10) {
                    lore.add("§7• §c§m" + talents.get(reqID).name + " Level 10");
                } else {
                    lore.add("§7• §e" + talents.get(reqID).name + " Level 10");
                }
            }
            if (hasRequiredLevel) {
                lore.add("§7• §eRequires Archaeology Level " + talent.requiredLevel);
            } else {
                lore.add("§7• §c§mArchaeology Level " + talent.requiredLevel);
            }
            meta.setLore(lore);
        }

        talentIcon.setItemMeta(meta);
        return talentIcon;
    }

    private boolean hasPrerequisites(List<Integer> requiredID, Map<Integer, Integer> talentTree) {
        for (int reqID : requiredID) {
            if (talentTree.getOrDefault(reqID, 0) < 10) {
                return false;
            }
        }
        return true;
    }

    private void openConfirmGUI(Player player, int talentID) {
        UUID uuid = player.getUniqueId();
        ChestGui confirmGui = new ChestGui(3, configManager.confirmGUITitle);
        StaticPane pane = new StaticPane(0, 0, 9, 3);
        confirmGui.setOnGlobalClick(event -> event.setCancelled(true));

        for (int i = 1; i <= 3; i++) {
            ItemStack confirmButton = createConfirmationButton();
            GuiItem confirm = new GuiItem(confirmButton, event -> {
                if (talentID <= 9) {
                    playerData.reduceTalentPoints(uuid, 1);
                } else {
                    playerData.reduceTalentPoints(uuid, 10);
                }
                talentTree.incrementTalentLevel(uuid, talentID);
                openTalentGUI(player);
            });
            pane.addItem(confirm, i, 1);
        }

        for (int i = 5; i <= 7; i++) {
            ItemStack cancelButton = createCloseButton();
            GuiItem confirm = new GuiItem(cancelButton, event -> {
                openTalentGUI(player);
            });
            pane.addItem(confirm, i, 1);
        }

        confirmGui.addPane(pane);
        confirmGui.show(player);
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createConfirmationButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aConfirm upgrade");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }
}

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
        ChestGui talentGui = new ChestGui(6, "Talent Trees");
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        Map<Integer, Integer> playerTalents = talentTree.getPlayerTalentMap(player.getUniqueId());
        int archLevel = playerData.getArchLevel(player.getUniqueId());

        List<Integer> sortedTalentIDs = new ArrayList<>(talents.keySet());
        Collections.sort(sortedTalentIDs);

        int index = 0;

        for (int talentID : sortedTalentIDs) {
            ConfigManager.Talent talent = talents.get(talentID);

            boolean hasLevelRequirement = archLevel >= talent.requiredLevel;

            boolean hasPrerequisiteTalent = true;
            if (!talent.requiredIDs.isEmpty()) {
                for (int reqId : talent.requiredIDs) {
                    int currentLevel = playerTalents.getOrDefault(reqId, 0);
                    if (currentLevel < 10) {
                        hasPrerequisiteTalent = false;
                        break;
                    }
                }
            }

            boolean unlocked = hasLevelRequirement && hasPrerequisiteTalent;

            ItemStack item;
            int currentTalentLevel = playerTalents.getOrDefault(talentID, 0);
            if (!unlocked) {
                item = new ItemStack(Material.BARRIER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(talent.name + " (Locked)");
                meta.setCustomModelData(100000);
                List<String> lore = new ArrayList<>();
                lore.add("Requires Arch Level: " + talent.requiredLevel);
                if (!talent.requiredIDs.isEmpty()) {
                    lore.add("Requires talents " + talent.requiredIDs + " at level 10");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            } else {
                if (currentTalentLevel == 0) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                } else {
                    item = new ItemStack(talent.material);
                }
                item.setAmount(currentTalentLevel > 0 ? currentTalentLevel : 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(talent.name + " (Level " + currentTalentLevel + ")");
                List<String> lore = new ArrayList<>();
                lore.add("Required Arch Level: " + talent.requiredLevel);
                if (!talent.requiredIDs.isEmpty()) {
                    lore.add("Requires talents " + talent.requiredIDs + " at level 10");
                }
                meta.setLore(lore);
                if (currentTalentLevel > 0) {
                    meta.setCustomModelData(talent.customModelData);
                }
                item.setItemMeta(meta);
            }

            GuiItem guiItem = new GuiItem(item, event -> {
                event.setCancelled(true);
                if (!unlocked) {
                    return;
                }
                if (playerData.getTalentPoints(player.getUniqueId()) > 0) {
                    openConfirmGUI(player, talentID);
                } else {
                    player.sendMessage("You don't have talent points!");
                }
            });

            int col = index % 9;
            int row = index / 9;
            pane.addItem(guiItem, col, row);
            index++;
        }

        talentGui.addPane(pane);
        talentGui.show(player);
    }

    public void openConfirmGUI(Player player, int talentID) {
        ChestGui confirmGui = new ChestGui(1, "Confirm Talent Upgrade");
        StaticPane pane = new StaticPane(0, 0, 9, 1);

        ItemStack confirmItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName("Confirm Upgrade");
        confirmItem.setItemMeta(confirmMeta);

        GuiItem confirmGuiItem = new GuiItem(confirmItem, event -> {
            event.setCancelled(true);
            playerData.decrementTalentPoints(player.getUniqueId());
            talentTree.incrementTalentLevel(player.getUniqueId(), talentID);
            openTalentGUI(player);
        });

        ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName("Cancel");
        cancelItem.setItemMeta(cancelMeta);

        GuiItem cancelGuiItem = new GuiItem(cancelItem, event -> {
            event.setCancelled(true);
            openTalentGUI(player);
        });

        pane.addItem(confirmGuiItem, 3, 0);
        pane.addItem(cancelGuiItem, 5, 0);

        confirmGui.addPane(pane);
        confirmGui.show(player);
    }
}

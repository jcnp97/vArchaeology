package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.*;

public class TraitGUI implements Listener {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private int wisdomPoints = 0;
    private int karmaPoints = 0;
    private int charismaPoints = 0;
    private int dexterityPoints = 0;
    private int traitPoints = 0;

    public TraitGUI(Main plugin, EffectsUtil effectsUtil, PlayerDataManager playerDataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openInfoMode(Player player) {
        ChestGui gui = new ChestGui(3, configManager.traitMenu);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = createInfoPane(player);

        gui.addPane(staticPane);
        gui.show(player);
    }

    private StaticPane createInfoPane(Player player) {
        UUID playerUUID = player.getUniqueId();
        int wisdomTrait = playerDataManager.getWisdomTrait(playerUUID);
        int karmaTrait = playerDataManager.getKarmaTrait(playerUUID);
        int charismaTrait = playerDataManager.getCharismaTrait(playerUUID);
        int dexterityTrait = playerDataManager.getDexterityTrait(playerUUID);
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);

        // toggle upgrade button
        if (playerDataManager.getTraitPoints(playerUUID) > 0) {
            for (int x = 1; x <= 3; x++) {
                ItemStack upgradeButton = createUpgradeButton();
                GuiItem guiItem = new GuiItem(upgradeButton, event -> openUpgradeMode(player));
                staticPane.addItem(guiItem, x, 1);
            }
        } else {
            for (int x = 1; x <= 3; x++) {
                ItemStack upgradeButton = createUpgradeButtonNTP();
                GuiItem guiItem = new GuiItem(upgradeButton);
                staticPane.addItem(guiItem, x, 1);
            }
        }
        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 2);
        }
        // traits
        ItemStack wisdomInfo = createWisdomInfo(wisdomTrait);
        ItemStack charismaInfo = createCharismaInfo(charismaTrait);
        ItemStack karmaInfo = createKarmaInfo(karmaTrait);
        ItemStack dexterityInfo = createDexterityInfo(dexterityTrait);
        staticPane.addItem(new GuiItem(wisdomInfo), 1, 0);
        staticPane.addItem(new GuiItem(charismaInfo), 3, 0);
        staticPane.addItem(new GuiItem(karmaInfo), 5, 0);
        staticPane.addItem(new GuiItem(dexterityInfo), 7, 0);

        return staticPane;
    }

    private ItemStack createWisdomInfo(int wisdomLevel) {
        ItemStack button = new ItemStack(Material.EMERALD, Math.max(wisdomLevel, 1));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§e§lWisdom Trait");
            meta.setCustomModelData(100000);
            meta.setLore(List.of(
                    "§7• Provides §a" + wisdomLevel*2 + "% §7Archaeology XP when",
                    "  §7breaking blocks.",
                    "§7• Provides §a" + wisdomLevel + "% §7Archaeology XP when",
                    "  §7receiving archaeology materials.",
                    "§7• Provides §a" + String.format("%.1f", (double) wisdomLevel*0.50) + "% §7Archaeology XP when",
                    "  §7restoring artefacts."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCharismaInfo(int charismaLevel) {
        ItemStack button = new ItemStack(Material.EMERALD, Math.max(charismaLevel, 1));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§9§lCharisma Trait");
            meta.setCustomModelData(100001);
            meta.setLore(List.of(
                    "§7• Provides §a" + charismaLevel + "% §7bonus sell price of",
                    "  §7Archaeology materials.",
                    "§7• Provides §a" + String.format("%.1f", (double) charismaLevel*0.25) + "% §7bonus sell price of",
                    "  §7restored artefacts."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createKarmaInfo(int karmaLevel) {
        ItemStack button = new ItemStack(Material.EMERALD, Math.max(karmaLevel, 1));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§2§lKarma Trait");
            meta.setCustomModelData(100002);
            meta.setLore(List.of(
                    "§7• Provides §a" + String.format("%.2f", (double) karmaLevel*0.05) + "% §7Gathering rate.",
                    "§7• Provides §a" + String.format("%.1f", (double) karmaLevel*0.2) + "% §7chance to trigger a",
                    "  §7double drop when receiving materials.",
                    "§7• Provides §a" + String.format("%.1f", (double) karmaLevel*0.2) + "% §7chance to increase",
                    "  §7the tier of received material by 1."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createDexterityInfo(int dexterityLevel) {
        ItemStack button = new ItemStack(Material.EMERALD, Math.max(dexterityLevel, 1));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§4§lDexterity Trait");
            meta.setCustomModelData(100003);
            meta.setLore(List.of(
                    "§7• Provides §a" + String.format("%.3f", (double) dexterityLevel*0.005) + "% §7Artefact Discovery.",
                    "§7• Provides §a" + String.format("%.1f", (double) dexterityLevel*0.1) + "% §7chance to trigger a",
                    "  §7double progress on Artefact Discovery.",
                    "§7• Provides §a" + String.format("%.2f", (double) dexterityLevel*0.02) + "% §7chance to increase",
                    "  §7Artefact Discovery progress by 1%."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createUpgradeButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aToggle Upgrade Mode");
            meta.setCustomModelData(configManager.cancelModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createUpgradeButtonNTP() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cYou do not have points to upgrade right now.");
            meta.setCustomModelData(configManager.cancelModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(configManager.cancelModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    // UPGRADE MODE GUI METHODS
    private void resetPoints() {
        wisdomPoints = 0;
        karmaPoints = 0;
        charismaPoints = 0;
        dexterityPoints = 0;
    }

    public void openUpgradeMode(Player player) {
        UUID playerUUID = player.getUniqueId();
        traitPoints = playerDataManager.getTraitPoints(playerUUID);

        resetPoints();
        // Create new GUI instance
        ChestGui gui = new ChestGui(3, "Attribute Upgrade");

        // Create background pane
        OutlinePane backgroundPane = new OutlinePane(0, 0, 9, 3);
        backgroundPane.addItem(new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
        backgroundPane.setRepeat(true);
        backgroundPane.setPriority(Pane.Priority.LOWEST);

        // Create static pane for attributes and buttons
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        // Add attribute items
        addAttributeItem(staticPane, 1, 1, "Wisdom", wisdomPoints, player, gui);
        addAttributeItem(staticPane, 3, 1, "Karma", karmaPoints, player, gui);
        addAttributeItem(staticPane, 5, 1, "Charisma", charismaPoints, player, gui);
        addAttributeItem(staticPane, 7, 1, "Dexterity", dexterityPoints, player, gui);

        // Create confirm button
        ItemStack confirmItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Upgrades");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Click to confirm your attribute upgrades");
        confirmLore.add(ChatColor.YELLOW + "Points remaining: " + traitPoints);
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);

        // Create cancel button
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to cancel upgrades"));
        cancelItem.setItemMeta(cancelMeta);

        // Add buttons
        staticPane.addItem(new GuiItem(confirmItem, event -> {
            playerDataManager.reduceTraitPoints(playerUUID, applyUpgrades(playerUUID));
            resetPoints();
            player.closeInventory();
        }), 4, 2);

        staticPane.addItem(new GuiItem(cancelItem, event -> {
            resetPoints();
            player.closeInventory();
        }), 8, 2);

        // Add panes to GUI
        gui.addPane(backgroundPane);
        gui.addPane(staticPane);

        // Show GUI to player
        gui.show(player);
    }

    private void addAttributeItem(StaticPane pane, int x, int y, String attributeName, int points, Player player, ChestGui gui) {
        Material material = points > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material, Math.max(1, points));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + attributeName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current points: " + points);
        lore.add(ChatColor.YELLOW + "Left-click to add point");
        lore.add(ChatColor.YELLOW + "Right-click to remove point");
        meta.setLore(lore);
        item.setItemMeta(meta);

        pane.addItem(new GuiItem(item, event -> {
            if (event.isLeftClick() && traitPoints > 0) {
                switch (attributeName) {
                    case "Wisdom":
                        wisdomPoints++;
                        break;
                    case "Karma":
                        karmaPoints++;
                        break;
                    case "Charisma":
                        charismaPoints++;
                        break;
                    case "Dexterity":
                        dexterityPoints++;
                        break;
                }
                traitPoints--;
                updateGUI(player, gui);
            } else if (event.isRightClick()) {
                switch (attributeName) {
                    case "Wisdom":
                        if (wisdomPoints > 0) {
                            wisdomPoints--;
                            traitPoints++;
                        }
                        break;
                    case "Luck":
                        if (karmaPoints > 0) {
                            karmaPoints--;
                            traitPoints++;
                        }
                        break;
                    case "Charisma":
                        if (charismaPoints > 0) {
                            charismaPoints--;
                            traitPoints++;
                        }
                        break;
                    case "Dexterity":
                        if (dexterityPoints > 0) {
                            dexterityPoints--;
                            traitPoints++;
                        }
                        break;
                }
                updateGUI(player, gui);
            }
            event.setCancelled(true);
        }), x, y);
    }

    private void updateGUI(Player player, ChestGui gui) {
        // Create new static pane with updated items
        UUID playerUUID = player.getUniqueId();
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        // Update attribute items
        addAttributeItem(staticPane, 1, 1, "Wisdom", wisdomPoints, player, gui);
        addAttributeItem(staticPane, 3, 1, "Karma", karmaPoints, player, gui);
        addAttributeItem(staticPane, 5, 1, "Charisma", charismaPoints, player, gui);
        addAttributeItem(staticPane, 7, 1, "Dexterity", dexterityPoints, player, gui);

        // Update confirm button
        ItemStack confirmItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Upgrades");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Click to confirm your attribute upgrades");
        confirmLore.add(ChatColor.YELLOW + "Points remaining: " + traitPoints);
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);

        staticPane.addItem(new GuiItem(confirmItem, event -> {
            playerDataManager.reduceTraitPoints(playerUUID, applyUpgrades(playerUUID));
            player.closeInventory();
        }), 4, 2);

        // Re-add cancel button
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to cancel upgrades"));
        cancelItem.setItemMeta(cancelMeta);

        staticPane.addItem(new GuiItem(cancelItem, event -> {
            player.closeInventory();
        }), 8, 2);

        // Clear and update GUI panes
        gui.getPanes().removeIf(pane -> pane instanceof StaticPane);
        gui.addPane(staticPane);
        gui.update();
    }

    private int applyUpgrades(UUID playerUUID) {
        int totalPoints = 0;
        if (wisdomPoints > 0) {
            playerDataManager.addWisdomTrait(playerUUID, wisdomPoints);
            totalPoints += wisdomPoints;
        }
        if (karmaPoints > 0) {
            playerDataManager.addKarmaTrait(playerUUID, karmaPoints);
            totalPoints += karmaPoints;
        }
        if (charismaPoints > 0) {
            playerDataManager.addCharismaTrait(playerUUID, charismaPoints);
            totalPoints += charismaPoints;
        }
        if (dexterityPoints > 0) {
            playerDataManager.addDexterityTrait(playerUUID, dexterityPoints);
            totalPoints += dexterityPoints;
        }
        return totalPoints;
    }
}
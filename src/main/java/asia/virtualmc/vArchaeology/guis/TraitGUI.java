package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
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
    private final PlayerData PlayerData;
    private final Map<String, Integer> traitPoints = new LinkedHashMap<>();
    private int remainingPoints = 0;
    private final int maxTraitLevel = 50;

    public TraitGUI(Main plugin, EffectsUtil effectsUtil, PlayerData PlayerData, ConfigManager configManager) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.PlayerData = PlayerData;
        this.configManager = configManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Initialize trait points map
        traitPoints.put("Wisdom", 0);
        traitPoints.put("Charisma", 0);
        traitPoints.put("Karma", 0);
        traitPoints.put("Dexterity", 0);
    }

    public void openInfoMode(Player player) {
        ChestGui gui = new ChestGui(3, configManager.traitGUITitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.addPane(createInfoPane(player));
        gui.show(player);
    }

    private StaticPane createInfoPane(Player player) {
        UUID playerUUID = player.getUniqueId();
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);
        int availablePoints = PlayerData.getTraitPoints(playerUUID);

        // Add traits buttons
        addTraitInfoItems(staticPane, playerUUID);

        // Add upgrade/close buttons
        addControlButtons(staticPane, player, availablePoints);

        return staticPane;
    }

    private void addTraitInfoItems(StaticPane pane, UUID playerUUID) {
        Map<String, ItemStack> traitItems = new LinkedHashMap<>();
        traitItems.put("Wisdom", createTraitInfo("Wisdom", PlayerData.getWisdomTrait(playerUUID), 100000));
        traitItems.put("Charisma", createTraitInfo("Charisma", PlayerData.getCharismaTrait(playerUUID), 100001));
        traitItems.put("Karma", createTraitInfo("Karma", PlayerData.getKarmaTrait(playerUUID), 100002));
        traitItems.put("Dexterity", createTraitInfo("Dexterity", PlayerData.getDexterityTrait(playerUUID), 100003));

        int[] positions = {1, 3, 5, 7};
        int i = 0;
        for (ItemStack item : traitItems.values()) {
            pane.addItem(new GuiItem(item), positions[i++], 0);
        }
    }

    private ItemStack createTraitInfo(String traitName, int level, int modelData) {
        ItemStack button = new ItemStack(Material.EMERALD, Math.max(level, 1));
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        meta.setCustomModelData(modelData);
        List<String> lore = new ArrayList<>();

        switch (traitName) {
            case "Wisdom":
                meta.setDisplayName("§e§lWisdom Trait");
                lore.addAll(Arrays.asList(
                        "§7• Provides §a" + level*2 + "% §7Archaeology XP when breaking blocks.",
                        "§7• Provides §a" + level + "% §7Archaeology XP when receiving materials.",
                        "§7• Provides §a" + String.format("%.1f", level*0.50) + "% §7XP from Artefact Restoration."
                ));
                break;
            case "Charisma":
                meta.setDisplayName("§9§lCharisma Trait");
                lore.addAll(Arrays.asList(
                        "§7• Provides §a" + level + "% §7bonus sell price of Archaeology materials.",
                        "§7• Provides §a" + String.format("%.1f", level*0.25) + "% §7bonus sell price of restored artefacts."
                ));
                break;
            case "Karma":
                meta.setDisplayName("§2§lKarma Trait");
                lore.addAll(Arrays.asList(
                        "§7• Provides §a" + String.format("%.2f", level*0.05) + "% §7Gathering rate.",
                        "§7• Provides §a" + String.format("%.1f", level*0.2) + "% §7chance for double drop.",
                        "§7• Provides §a" + String.format("%.1f", level*0.2) + "% §7chance to increase material tier."
                ));
                break;
            case "Dexterity":
                meta.setDisplayName("§4§lDexterity Trait");
                lore.addAll(Arrays.asList(
                        "§7• Provides §a" + String.format("%.3f", level*0.005) + "% §7Artefact Discovery.",
                        "§7• Provides §a" + String.format("%.1f", level*0.1) + "% §7chance for double progress.",
                        "§7• Provides §a" + String.format("%.2f", level*0.02) + "% §7chance to increase progress."
                ));
                break;
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    private void addControlButtons(StaticPane pane, Player player, int availablePoints) {
        // Add upgrade buttons
        ItemStack upgradeButton = createButton(
                availablePoints > 0 ? "§6Toggle Upgrade Mode §7(§ePoints Left: §a" + availablePoints + "§7)" : "§cYou do not have points to upgrade right now.",
                Material.PAPER,
                configManager.invisibleModelData
        );

        for (int x = 1; x <= 3; x++) {
            GuiItem guiItem = availablePoints > 0
                    ? new GuiItem(upgradeButton, event -> openUpgradeMode(player))
                    : new GuiItem(upgradeButton);
            pane.addItem(guiItem, x, 2);
        }

        // Add close buttons
        ItemStack closeButton = createButton("§cClose", Material.PAPER, configManager.invisibleModelData);
        for (int x = 5; x <= 7; x++) {
            pane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 2);
        }
    }

    private ItemStack createButton(String name, Material material, int modelData) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setCustomModelData(modelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private void resetPoints() {
        traitPoints.replaceAll((k, v) -> 0);
    }

    public void openUpgradeMode(Player player) {
        UUID playerUUID = player.getUniqueId();
        remainingPoints = PlayerData.getTraitPoints(playerUUID);
        resetPoints();

        ChestGui gui = new ChestGui(3, configManager.traitGUIUpgrade);
        setupUpgradeGui(gui, player);
        gui.show(player);
    }

    private void setupUpgradeGui(ChestGui gui, Player player) {
        // Static pane for attributes and buttons
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        // Add trait items
        int[] positions = {1, 3, 5, 7};
        int i = 0;
        for (Map.Entry<String, Integer> entry : traitPoints.entrySet()) {
            addTraitItem(staticPane, positions[i++], 0, entry.getKey(), player, gui);
        }
        // Add control buttons
        addUpgradeControlButtons(staticPane, player);
        gui.addPane(staticPane);
    }

    private void addUpgradeControlButtons(StaticPane pane, Player player) {
        UUID playerUUID = player.getUniqueId();
        // Confirm buttons (3 copies)
        ItemStack confirmItem = createButton("Confirm", Material.PAPER, configManager.invisibleModelData);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm §7(§eRemaining Points: " + remainingPoints + "§7)");
            confirmItem.setItemMeta(confirmMeta);
        }
        for (int x = 1; x <= 3; x++) {
            pane.addItem(new GuiItem(confirmItem, event -> {
                int totalPoints = applyUpgrades(playerUUID);
                if (totalPoints > 0) {
                    PlayerData.reduceTraitPoints(playerUUID, totalPoints);
                    player.closeInventory();
                    effectsUtil.playSoundUUID(playerUUID, "minecraft:entity.player.levelup", Sound.Source.PLAYER, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§cYou did not add any upgrade points to any of your traits.");
                    player.closeInventory();
                }
            }), x, 2);
        }

        // Cancel buttons (3 copies)
        ItemStack cancelItem = createButton("§cCancel", Material.PAPER, configManager.invisibleModelData);
        for (int x = 5; x <= 7; x++) {
            pane.addItem(new GuiItem(cancelItem, event -> {
                resetPoints();
                openInfoMode((Player) event.getWhoClicked());
            }), x, 2);
        }
    }

    private void addTraitItem(StaticPane pane, int x, int y, String traitName, Player player, ChestGui gui) {
        int points = traitPoints.get(traitName);
        ItemStack item = new ItemStack(
                points > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                Math.max(1, points)
        );

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + traitName + " §7(§a+" + points + "§7)");

            int currentLevel = getCurrentTraitLevel(player.getUniqueId(), traitName);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Left-click to add point");
            lore.add(ChatColor.YELLOW + "Right-click to remove point");
            //lore.add(ChatColor.GRAY + "Current Level: " + currentLevel + "/" + maxTraitLevel);

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        pane.addItem(new GuiItem(item, event -> {
            if (event.isLeftClick() && remainingPoints > 0) {
                int currentLevel = getCurrentTraitLevel(player.getUniqueId(), traitName);
                if (currentLevel + traitPoints.get(traitName) < maxTraitLevel) {
                    traitPoints.compute(traitName, (k, v) -> v + 1);
                    remainingPoints--;
                } else {
                    player.sendMessage(ChatColor.RED + "This trait has reached its maximum level of " + maxTraitLevel + "!");
                }
            } else if (event.isRightClick() && traitPoints.get(traitName) > 0) {
                traitPoints.compute(traitName, (k, v) -> v - 1);
                remainingPoints++;
            }
            updateGUI(player, gui);
            event.setCancelled(true);
        }), x, y);
    }

    private int getCurrentTraitLevel(UUID playerUUID, String traitName) {
        switch (traitName) {
            case "Wisdom":
                return PlayerData.getWisdomTrait(playerUUID);
            case "Karma":
                return PlayerData.getKarmaTrait(playerUUID);
            case "Charisma":
                return PlayerData.getCharismaTrait(playerUUID);
            case "Dexterity":
                return PlayerData.getDexterityTrait(playerUUID);
            default:
                return 0;
        }
    }

    private void updateGUI(Player player, ChestGui gui) {
        gui.getPanes().clear();
        setupUpgradeGui(gui, player);
        gui.update();
    }

    private int applyUpgrades(UUID playerUUID) {
        int totalPoints = 0;
        Map<String, Integer> pointsMap = new HashMap<>(traitPoints);

        if (pointsMap.get("Wisdom") > 0) {
            PlayerData.addWisdomTrait(playerUUID, pointsMap.get("Wisdom"));
            totalPoints += pointsMap.get("Wisdom");
        }
        if (pointsMap.get("Karma") > 0) {
            PlayerData.addKarmaTrait(playerUUID, pointsMap.get("Karma"));
            totalPoints += pointsMap.get("Karma");
        }
        if (pointsMap.get("Charisma") > 0) {
            PlayerData.addCharismaTrait(playerUUID, pointsMap.get("Charisma"));
            totalPoints += pointsMap.get("Charisma");
        }
        if (pointsMap.get("Dexterity") > 0) {
            PlayerData.addDexterityTrait(playerUUID, pointsMap.get("Dexterity"));
            totalPoints += pointsMap.get("Dexterity");
        }
        return totalPoints;
    }
}
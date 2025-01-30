package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.items.ArtefactItems;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class ArtefactRestorationGUI {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final EXPManager expManager;
    private final PlayerData playerData;
    private final Statistics statistics;
    private final ArtefactItems artefactItems;
    private final NamespacedKey ARTEFACT_KEY;

    public ArtefactRestorationGUI(Main plugin,
                                  EffectsUtil effectsUtil,
                                  EXPManager expManager,
                                  ArtefactItems artefactItems,
                                  PlayerData playerData,
                                  Statistics statistics) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.expManager = expManager;
        this.artefactItems = artefactItems;
        this.playerData = playerData;
        this.statistics = statistics;
        this.ARTEFACT_KEY = new NamespacedKey(plugin, "varch_artefact");
    }

    public void openRestoreArtefact(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (artefactItems.getArtefactID(item) == 0) {
            player.sendMessage("§cError: Artefact not found in your main hand. Please try again.");
            return;
        }

        UUID uuid = player.getUniqueId();
        double initialXP = expManager.getTotalArtefactRestoreEXP(uuid);
        int archLevel = playerData.getArchLevel(uuid);

        ChestGui gui = new ChestGui(4, "§fTest");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 2);

        // type-1 restoration (Lv. 30)
        if (archLevel >= 30) {
            for (int x = 1; x <= 3; x++) {
                ItemStack confirmButton = createType1Button(initialXP * 0.30);
                GuiItem guiItem = new GuiItem(confirmButton, event -> openRestoreT1ArtefactConfirm(player, initialXP * 0.30));
                staticPane.addItem(guiItem, x, 0);
            }
        } else {
            for (int x = 1; x <= 3; x++) {
                ItemStack confirmButton = createNoAccessType1();
                GuiItem guiItem = new GuiItem(confirmButton);
                staticPane.addItem(guiItem, x, 0);
            }
        }

        // type-2 restoration (Lv. 60)
        if (archLevel >= 60) {
            ArrayList<Integer> componentsOwned = new ArrayList<>(statistics.getComponents(uuid));
            for (int x = 5; x <= 7; x++) {
                ItemStack confirmButton = createType2Button(initialXP, componentsOwned);
                GuiItem guiItem = new GuiItem(confirmButton, event -> openRestoreT2ArtefactConfirm(player, initialXP));
                staticPane.addItem(guiItem, x, 0);
            }
        } else {
            for (int x = 5; x <= 7; x++) {
                ItemStack confirmButton = createNoAccessType2();
                GuiItem guiItem = new GuiItem(confirmButton);
                staticPane.addItem(guiItem, x, 0);
            }
        }

        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    public void openRestoreT1ArtefactConfirm(Player player, double initialXP) {
        ChestGui gui = new ChestGui(4, "§fTest");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 2);

        // type-1 restoration (Lv. 30)
        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createType1Button(initialXP * 0.30);
            GuiItem guiItem = new GuiItem(confirmButton, event -> processType1Restore(player, initialXP * 0.30));
            staticPane.addItem(guiItem, x, 0);
        }

        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    public void openRestoreT2ArtefactConfirm(Player player, double initialXP) {
        ChestGui gui = new ChestGui(4, "§fTest");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 2);

        // type-2 restoration (Lv. 60)
        for (int x = 5; x <= 7; x++) {
            ItemStack confirmButton = createConfirmationButton();
            GuiItem guiItem = new GuiItem(confirmButton, event -> processType2Restore(player, initialXP));
            staticPane.addItem(guiItem, x, 0);
        }

        // close button
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createType1Button(double exp) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eType-1 Restoration §7(§a+" + formattedXP + " XP§7)");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§cDOES NOT REQUIRE §7components to restore an",
                    "§7artefact but will most likely destroy it during",
                    "§7the restoration process.",
                    "",
                    "§4§lWARNING! §cThis will completely destroy your artefact."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createType2Button(double exp, ArrayList<Integer> componentsOwned) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eType-2 Restoration §7(§a+" + formattedXP + " XP§7)");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§7This require the following components",
                    "§7to ensure a successful restoration process.",
                    "",
                    "§7• §aCommon Components: §2" + componentsOwned.get(0) + "§7/§c64",
                    "§7• §bUncommon Components: §2" + componentsOwned.get(1) + "§7/§c32",
                    "§7• §3Rare Components: §2" + componentsOwned.get(2) + "§7/§c16",
                    "§7• §eUnique Components: §2" + componentsOwned.get(3) + "§7/§c12",
                    "§7• §6Special Components: §2" + componentsOwned.get(4) + "§7/§c8",
                    "§7• §5Mythical Components: §2" + componentsOwned.get(5) + "§7/§c4",
                    "§7• §4Exotic Components: §2" + componentsOwned.get(6) + "§7/§c2"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processType1Restore(Player player, double initialXP) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();
        double finalXP = expManager.getTotalArtefactRestoreEXP(uuid) * 0.30;

        if (initialXP != finalXP) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        if (artefactItems.getArtefactID(item) == 0) {
            player.sendMessage("§cError: Artefact not found in your main hand. Please try again.");
            player.closeInventory();
            return;
        }

        try {
            if (finalXP > 0) {
                player.getInventory().removeItem(item);
                expManager.addRestorationXP(uuid, finalXP);
            } else {
                player.sendMessage("§cThere was an error processing the action. Please contact the administrator.");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing the restoration. Please try again.");
            plugin.getLogger().severe("Error processing artefact restoration for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
    }

    private void processType2Restore(Player player, double initialXP) {
        UUID uuid = player.getUniqueId();
        double finalXP = expManager.getTotalArtefactRestoreEXP(uuid);
        ItemStack item = player.getInventory().getItemInMainHand();

        if (initialXP != finalXP) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        if (artefactItems.getArtefactID(item) == 0) {
            player.sendMessage("§cError: Artefact not found in your main hand. Please try again.");
            player.closeInventory();
            return;
        }

        ArrayList<Integer> componentsOwned = new ArrayList<>(statistics.getComponents(uuid));
        List<Integer> componentsRequired = Arrays.asList(64, 32, 16, 12, 8, 4, 2);

        if (componentsOwned.size() == componentsRequired.size()) {
            for (int i = 0; i < componentsOwned.size(); i++) {
                if (componentsOwned.get(i) < componentsRequired.get(i)) {
                    player.sendMessage("§cError: You do not have the required number of components to do this.");
                    player.closeInventory();
                    return;
                }
            }
        }

        try {
            if (finalXP > 0) {
                statistics.subtractComponents(uuid, componentsRequired);
                player.getInventory().removeItem(item);
                expManager.addRestorationXP(uuid, finalXP);
            } else {
                player.sendMessage("§cThere was an error processing the action. Please contact the administrator.");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing the restoration. Please try again.");
            plugin.getLogger().severe("Error processing artefact restoration for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createConfirmationButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aConfirm process.");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoAccessType1() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cUnlocked at Lv. 30");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoAccessType2() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cUnlocked at Lv. 60");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }
}
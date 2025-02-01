package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.ArtefactCollections;
import asia.virtualmc.vArchaeology.items.ArtefactItems;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CollectionLogGUI {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final ConfigManager configManager;
    private final ArtefactItems artefactItems;
    private final ArtefactCollections artefactCollections;
    private final CollectionLog collectionLog;
    // pre-building variables
    private final int totalCollections;
    private final int itemPerPage;
    private final int totalPages;

    public CollectionLogGUI(Main plugin,
                            EffectsUtil effectsUtil,
                            ConfigManager configManager,
                            ArtefactItems artefactItems,
                            ArtefactCollections artefactCollections,
                            CollectionLog collectionLog
                            ) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.configManager = configManager;
        this.artefactItems = artefactItems;
        this.artefactCollections = artefactCollections;
        this.collectionLog = collectionLog;
        // pre-building our GUI on init
        this.totalCollections = collectionLog.totalCollections;
        this.itemPerPage = 45;
        this.totalPages = (totalCollections + itemPerPage - 1) / itemPerPage;
    }

    public void openCollectionLog(Player player, int pageNumber) {
        UUID uuid = player.getUniqueId();
        Map<Integer, Integer> playerCollection = collectionLog.getPlayerCollection(uuid);

        ChestGui gui = new ChestGui(6, "Collection Log");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 6);

        int itemID = (pageNumber - 1) * itemPerPage;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; i++) {
                itemID++;
                int amount = playerCollection.get(itemID);
                if (amount > 0) {
                    ItemStack collectionItem = createCollectionItem(itemID, amount);
                    staticPane.addItem(new GuiItem(collectionItem), i, j);
                } else {
                    ItemStack collectionItem = createCollectionItemNew(itemID);
                    staticPane.addItem(new GuiItem(collectionItem), i, j);
                }
            }
            itemID++;
        }

        if (pageNumber == 1) {
            ItemStack nextButton = createNextButton();
            GuiItem guiItem = new GuiItem(nextButton, event -> openCollectionLog(player, pageNumber + 1));
            staticPane.addItem(guiItem, 1, 5);
        } else if (pageNumber == totalPages) {
            ItemStack previousButton = createPreviousButton();
            GuiItem guiItem = new GuiItem(previousButton, event -> openCollectionLog(player, pageNumber - 1));
            staticPane.addItem(guiItem, 7, 5);
        } else {
            ItemStack nextButton = createNextButton();
            GuiItem guiItem = new GuiItem(nextButton, event -> openCollectionLog(player, pageNumber + 1));
            staticPane.addItem(guiItem, 1, 5);

            ItemStack previousButton = createPreviousButton();
            GuiItem guiItem2 = new GuiItem(previousButton, event -> openCollectionLog(player, pageNumber - 1));
            staticPane.addItem(guiItem2, 7, 5);
        }

        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 5);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createCollectionItem(int collectionID, int amount) {
        ItemStack button = new ItemStack(Material.FLINT);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            if (collectionID > 0 && collectionID <= 7) {
                meta.setDisplayName(artefactItems.getDisplayName(collectionID));
                meta.setCustomModelData(99999 + collectionID);
                meta.setLore(configManager.rarityLore.get(collectionID));
                meta.setLore(List.of(
                        configManager.acquiredLore + " §e" + amount
                ));
                button.setItemMeta(meta);
            } else {
                meta.setDisplayName(artefactCollections.getDisplayName(collectionID));
                meta.setCustomModelData(configManager.startingModelData + collectionID);
                meta.setLore(List.of(
                        configManager.acquiredLore + " §e" + amount
                ));
                button.setItemMeta(meta);
            }

        }
        return button;
    }

    private ItemStack createCollectionItemNew(int collectionID) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            if (collectionID > 0 && collectionID <= 7) {
                meta.setDisplayName(artefactItems.getDisplayName(collectionID));
                meta.setCustomModelData(1);
                meta.setLore(configManager.rarityLore.get(collectionID));
                button.setItemMeta(meta);
            } else {
                meta.setDisplayName(artefactCollections.getDisplayName(collectionID));
                meta.setCustomModelData(1);
                meta.setLore(configManager.groupLore.get(collectionID));
                button.setItemMeta(meta);
            }

        }
        return button;
    }

    private ItemStack createNextButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aNext Page");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createPreviousButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aPrevious Page");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
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
}

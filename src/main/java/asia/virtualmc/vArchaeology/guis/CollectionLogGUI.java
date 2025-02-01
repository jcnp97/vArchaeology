package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.ArtefactCollections;
import asia.virtualmc.vArchaeology.items.ArtefactItems;
import asia.virtualmc.vArchaeology.items.CustomItems;
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
    private final CustomItems customItems;
    private final ArtefactCollections artefactCollections;
    private final CollectionLog collectionLog;
    private final int totalCollections;
    private final int itemPerPage;
    private final int totalPages;

    private static final int GUI_ROWS = 6;
    private static final int GUI_COLUMNS = 9;
    private static final int CONTENT_ROWS = 5;

    public CollectionLogGUI(Main plugin,
                            EffectsUtil effectsUtil,
                            ConfigManager configManager,
                            CustomItems customItems,
                            ArtefactCollections artefactCollections,
                            CollectionLog collectionLog) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.configManager = configManager;
        this.customItems = customItems;
        this.artefactCollections = artefactCollections;
        this.collectionLog = collectionLog;
        this.totalCollections = collectionLog.totalCollections;
        this.itemPerPage = CONTENT_ROWS * GUI_COLUMNS;
        this.totalPages = Math.max(1, (totalCollections + itemPerPage - 1) / itemPerPage);
    }

    public void openCollectionLog(Player player, int pageNumber) {
        pageNumber = Math.min(Math.max(1, pageNumber), totalPages);
        UUID uuid = player.getUniqueId();
        Map<Integer, Integer> playerCollection = collectionLog.getPlayerCollection(uuid);

        ChestGui gui = new ChestGui(GUI_ROWS, getGUIDesign(pageNumber));
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, GUI_COLUMNS, GUI_ROWS);

        // Add collection items
        int startIndex = (pageNumber - 1) * itemPerPage;
        for (int row = 0; row < CONTENT_ROWS; row++) {
            for (int col = 0; col < GUI_COLUMNS; col++) {
                int itemID = startIndex + (row * GUI_COLUMNS) + col + 1;
                if (itemID <= totalCollections) {
                    Integer amount = playerCollection.getOrDefault(itemID, 0);
                    ItemStack collectionItem = amount > 0
                            ? createCollectionItem(itemID, amount)
                            : createCollectionItemNew(itemID);
                    staticPane.addItem(new GuiItem(collectionItem), col, row);
                }
            }
        }

        addNavigationButtons(staticPane, pageNumber, player);

        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 5);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private void addNavigationButtons(StaticPane pane, int currentPage, Player player) {
        if (currentPage < totalPages) {
            ItemStack nextButton = createNextButton();
            GuiItem nextItem = new GuiItem(nextButton, event -> openCollectionLog(player, currentPage + 1));
            pane.addItem(nextItem, 6, 5);
        }

        if (currentPage > 1) {
            ItemStack previousButton = createPreviousButton();
            GuiItem prevItem = new GuiItem(previousButton, event -> openCollectionLog(player, currentPage - 1));
            pane.addItem(prevItem, 2, 5);
        }
    }

    private String getGUIDesign(int pageNumber) {
        if (pageNumber == 1) {
            return configManager.collectionLogNext;
        } else if (pageNumber == totalPages) {
            return configManager.collectionLogPrev;
        } else {
            return configManager.collectionLogBoth;
        }
    }

    private ItemStack createCollectionItem(int collectionID, int amount) {
        ItemStack button = new ItemStack(Material.FLINT);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        if (collectionID > 0 && collectionID <= 7) {
            meta.setDisplayName(customItems.getDisplayName(collectionID));
            meta.setCustomModelData(99999 + collectionID);
            List<String> lore = new ArrayList<>(configManager.rarityLore.get(collectionID));
            lore.add(configManager.acquiredLore + "§e" + amount);
            meta.setLore(lore);
        } else {
            meta.setDisplayName(artefactCollections.getDisplayName(collectionID - 7));
            meta.setCustomModelData(configManager.startingModelData + collectionID - 8);
            meta.setLore(Collections.singletonList(configManager.acquiredLore + "§e" + amount));
        }
        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createCollectionItemNew(int collectionID) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        if (collectionID > 0 && collectionID <= 7) {
            meta.setDisplayName(customItems.getDisplayName(collectionID));
            meta.setCustomModelData(100020);
            meta.setLore(configManager.rarityLore.get(collectionID));
        } else {
            meta.setDisplayName(artefactCollections.getDisplayName(collectionID - 7));
            meta.setCustomModelData(100020);
            meta.setLore(configManager.groupLore.get(artefactCollections.getGroupID(collectionID - 7)));
        }

        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createNextButton() {
        return createButton("§aNext Page", configManager.invisibleModelData);
    }

    private ItemStack createPreviousButton() {
        return createButton("§aPrevious Page", configManager.invisibleModelData);
    }

    private ItemStack createCloseButton() {
        return createButton("§cClose", configManager.invisibleModelData);
    }

    private ItemStack createButton(String displayName, int modelData) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setCustomModelData(modelData);
            button.setItemMeta(meta);
        }
        return button;
    }
}
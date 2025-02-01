package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.ArtefactCollections;
import asia.virtualmc.vArchaeology.items.ArtefactItems;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
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
    private static final int ITEMS_PER_PAGE = 45;
    private final int TOTAL_ITEMS;

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
        this.TOTAL_ITEMS = collectionLog.totalCollections;
    }

    private void openCollectionLogGUI(Player player, int page) {
        UUID uuid = player.getUniqueId();
        Map<Integer, Integer> playerCollection = collectionLog.getPlayerCollection(uuid);

        int totalPages = (TOTAL_ITEMS + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (page < 0 || page >= totalPages) {
            page = 0;
        }
        final int currentPage = page;

        ChestGui gui = new ChestGui(6, "Collection Log");

        PaginatedPane itemsPane = new PaginatedPane(0, 0, 9, 5);
        List<GuiItem> guiItems = new ArrayList<>();

        int startID = currentPage * ITEMS_PER_PAGE + 1;
        int endID = Math.min(TOTAL_ITEMS, (currentPage + 1) * ITEMS_PER_PAGE);
        int slot = 0;
        for (int itemID = startID; itemID <= endID; itemID++) {
            GuiItem item = createCollectionItem(itemID, playerCollection);
            int col = slot % 9;
            int row = slot / 9;
            itemsPane.addPane(item, col, row);
            slot++;
        }
        gui.addPane(itemsPane);

        // Create a static pane for navigation (the bottom row)
        StaticPane navPane = new StaticPane(0, 5, 9, 1);

        // Add the Previous Page button (if not on the first page)
        if (currentPage > 0) {
            GuiItem prevItem = new GuiItem(createNavItem(Material.ARROW, "Previous Page"), event -> {
                event.setCancelled(true);
                openCollectionLogGUI(player, currentPage - 1);
            });
            navPane.addItem(prevItem, 3, 0); // position (3,0) within the nav pane
        }

        // Add the Exit button (always shown)
        GuiItem exitItem = new GuiItem(createNavItem(Material.BARRIER, "Exit"), event -> {
            event.setCancelled(true);
            player.closeInventory();
        });
        navPane.addItem(exitItem, 4, 0);

        // Add the Next Page button (if not on the last page)
        if (currentPage < totalPages - 1) {
            GuiItem nextItem = new GuiItem(createNavItem(Material.ARROW, "Next Page"), event -> {
                event.setCancelled(true);
                openCollectionLogGUI(player, currentPage + 1);
            });
            navPane.addItem(nextItem, 5, 0);
        }
        gui.addPane(navPane);
        gui.show(player);
    }

    private GuiItem createCollectionItem(int itemID, Map<Integer, Integer> playerCollection) {
        int amount = playerCollection.getOrDefault(itemID, 0);
        String displayName;
        int customModelData;
        List<String> lore = new ArrayList<>();

        if (itemID >= 1 && itemID <= 7) {
            // For items 1–7 use artefactItems
            displayName = artefactItems.getDisplayName(itemID);
            customModelData = 100000 + (itemID - 1);
            if (amount > 0) {
                // Append acquired lore with the acquired amount
                for (String line : configManager.acquiredLore) {
                    lore.add(line + amount);
                }
            } else {
                // Use the by-rarity lore for this rarity
                lore = configManager.rarityLore.getOrDefault(itemID, new ArrayList<>());
            }
        } else {
            // For items 8+ use artefactCollections
            displayName = artefactCollections.getDisplayName(itemID);
            customModelData = configManager.startingModelData + (itemID - 8);
            if (amount > 0) {
                for (String line : configManager.acquiredLore) {
                    lore.add(line + amount);
                }
            } else {
                int groupID = artefactCollections.getGroupID(itemID);
                lore = configManager.groupLore.getOrDefault(groupID, new ArrayList<>());
            }
        }

        ItemStack itemStack = new ItemStack(Material.FLINT);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.setCustomModelData(customModelData);
            itemStack.setItemMeta(meta);
        }

        return new GuiItem(itemStack, event -> event.setCancelled(true));
    }

    private static ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}

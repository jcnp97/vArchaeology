package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.blocks.CraftingStation;
import asia.virtualmc.vArchaeology.logs.CraftingLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CraftingMaterials {
    private final Main plugin;
    private final CraftingLog craftingLog;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    private final Map<Integer, ItemStack> craftingCache;
    private static final String CRAFT_LIST_PATH = "craftingList";
    private final NamespacedKey CRAFT_KEY;

    public CraftingMaterials(Main plugin, CraftingLog craftingLog) {
        this.plugin = plugin;
        this.craftingLog = craftingLog;
        this.craftingCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "items/crafting-materials.yml");
        this.CRAFT_KEY = new NamespacedKey(plugin, "varch_crafting");

        createCraftingFile();
        loadCrafting();
    }

    private void createCraftingFile() {
        try {
            if (!customItemsFile.exists()) {
                customItemsFile.getParentFile().mkdirs();
                plugin.saveResource("items/crafting-materials.yml", false);
            }
            customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to create crafting item file: ", e);
        }
    }

    private void loadCrafting() {
        try {
            ConfigurationSection itemsList = customItemsConfig.getConfigurationSection(CRAFT_LIST_PATH);
            if (itemsList == null) {
                plugin.getLogger().warning("No items found in items/crafting-materials.yml");
                return;
            }
            for (String itemName : itemsList.getKeys(false)) {
                String path = CRAFT_LIST_PATH + "." + itemName;
                int id = customItemsConfig.getInt(path + ".id", -1);

                if (id == -1) {
                    plugin.getLogger().warning("Missing ID for crafting item: " + itemName);
                    continue;
                }
                ItemStack item = createCraftingMaterials(itemName, id);
                if (item != null) {
                    craftingCache.put(id, item.clone());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to load into cache: " + craftingCache.keySet());
        }
    }

    private ItemStack createCraftingMaterials(String itemName, int id) {
        try {
            String path = CRAFT_LIST_PATH + "." + itemName;
            String materialName = customItemsConfig.getString(path + ".material");
            String name = customItemsConfig.getString(path + ".name");
            int customModelData = customItemsConfig.getInt(path + ".custom-model-data");
            List<String> lore = customItemsConfig.getStringList(path + ".lore");

            if (materialName == null || name == null) {
                plugin.getLogger().warning("Invalid configuration for item: " + itemName);
                return null;
            }

            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(name.replace("&", "§"));
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§"));
                }
                meta.setLore(coloredLore);
                meta.setCustomModelData(customModelData);

                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(CRAFT_KEY, PersistentDataType.INTEGER, id);

                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create item: " + itemName, e);
            return null;
        }
    }

    public int getCraftingID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(CRAFT_KEY, PersistentDataType.INTEGER, 0);
    }

    public String getDisplayName(int craftID) {
        ItemStack item = craftingCache.get(craftID);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return "Unknown Item";
    }

    public ItemStack getCraftingMaterialCache(int craftID) {
        return craftingCache.get(craftID);
    }

    public void giveCraftingMaterial(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = craftingCache.get(id);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + id);
            return;
        }
        ItemStack giveItem = item.clone();
        giveItem.setAmount(Math.min(amount, giveItem.getMaxStackSize()));
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);

        craftingLog.logTransactionReceived(player.getName(), giveItem.getItemMeta().getDisplayName(), amount);

        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§eYour inventory was full. Some items were dropped at your feet.");
        }
    }

    public void removeCraftingMaterial(Player player, int craftID) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(CRAFT_KEY, PersistentDataType.INTEGER)) {
                continue;
            }

            Integer itemCraftId = pdc.get(CRAFT_KEY, PersistentDataType.INTEGER);
            if (itemCraftId == null || itemCraftId != craftID) {
                continue;
            }

            ItemStack removalItem = item.clone();
            removalItem.setAmount(1);

            HashMap<Integer, ItemStack> remaining = inventory.removeItem(removalItem);
            if (remaining.isEmpty()) {
                craftingLog.logTransactionTaken(player.getName(), meta.getDisplayName(), 1);
                break;
            }
        }
    }

    public boolean[] checkTimeSpaceFragmentMaterials(Player player) {
        boolean[] found = new boolean[7];

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (!meta.getPersistentDataContainer().has(CRAFT_KEY, PersistentDataType.INTEGER)) continue;

            int craftValue = meta.getPersistentDataContainer().get(CRAFT_KEY, PersistentDataType.INTEGER);
            if (craftValue >= 1 && craftValue <= 6) {
                found[craftValue] = true;
            }
        }

        for (int i = 1; i <= 6; i++) {
            if (!found[i]) {
                return found;
            }
        }
        return found;
    }

    public boolean hasTimeSpaceFragmentMaterials(Player player) {
        boolean[] found = new boolean[7];

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (!meta.getPersistentDataContainer().has(CRAFT_KEY, PersistentDataType.INTEGER)) continue;

            int craftValue = meta.getPersistentDataContainer().get(CRAFT_KEY, PersistentDataType.INTEGER);
            if (craftValue >= 1 && craftValue <= 6) {
                found[craftValue] = true;
            }
        }

        for (int i = 1; i <= 6; i++) {
            if (!found[i]) {
                return false;
            }
        }
        return true;
    }

    public void removeTimeSpaceFragmentMaterials(Player player) {
        for (int required = 1; required <= 6; required++) {
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || !item.hasItemMeta()) continue;

                ItemMeta meta = item.getItemMeta();
                if (!meta.getPersistentDataContainer().has(CRAFT_KEY, PersistentDataType.INTEGER)) continue;

                int craftValue = meta.getPersistentDataContainer().get(CRAFT_KEY, PersistentDataType.INTEGER);
                if (craftValue == required) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItem(slot, null);
                    }
                    break;
                }
            }
        }
        player.updateInventory();
    }

    public boolean hasCraftingMaterial(Player player, int craftID) {
        for (ItemStack item: player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(CRAFT_KEY, PersistentDataType.INTEGER)) {
                int itemID = meta.getPersistentDataContainer().get(CRAFT_KEY, PersistentDataType.INTEGER);
                if (itemID == craftID) {
                    return true;
                }
            }
        }
        return false;
    }

    public void reloadCharms() {
        craftingCache.clear();
        createCraftingFile();
        loadCrafting();
    }
}
package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CustomItems {
    private final Main plugin;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    private final Map<Integer, ItemStack> itemCache;
    private static final String ITEM_LIST_PATH = "itemsList";
    private final NamespacedKey ITEM_KEY;

    public CustomItems(Main plugin) {
        this.plugin = plugin;
        this.itemCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "custom-items.yml");
        this.ITEM_KEY = new NamespacedKey(plugin, "varch_item");

        createItemFile();
        loadItems();
    }

    private void createItemFile() {
        try {
            if (!customItemsFile.exists()) {
                customItemsFile.getParentFile().mkdirs();
                plugin.saveResource("custom-items.yml", false);
            }
            customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create custom items file", e);
        }
    }

    private void loadItems() {
        ConfigurationSection itemsList = customItemsConfig.getConfigurationSection(ITEM_LIST_PATH);
        if (itemsList == null) {
            plugin.getLogger().warning("No items found in custom-items.yml");
            return;
        }
        for (String itemName : itemsList.getKeys(false)) {
            String path = ITEM_LIST_PATH + "." + itemName;
            int id = customItemsConfig.getInt(path + ".id", -1);

            if (id == -1) {
                plugin.getLogger().warning("Missing ID for item: " + itemName);
                continue;
            }
            ItemStack item = createArchItemInternal(itemName, id);
            if (item != null) {
                itemCache.put(id, item.clone());
            }
        }
        plugin.getLogger().info("Loaded items into cache: " + itemCache.keySet());
    }

    private ItemStack createArchItemInternal(String itemName, int id) {
        try {
            String path = ITEM_LIST_PATH + "." + itemName;
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
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                // Set PersistentDataContainer data
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(ITEM_KEY, PersistentDataType.INTEGER, id);

                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create item: " + itemName, e);
            return null;
        }
    }

    public Integer getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(ITEM_KEY, PersistentDataType.INTEGER);
    }

    public void giveArchItem(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = itemCache.get(id);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + id);
            return;
        }

        int maxStackSize = item.getMaxStackSize();
        List<ItemStack> itemsToGive = new ArrayList<>();

        while (amount > 0) {
            int stackSize = Math.min(amount, maxStackSize);
            amount -= stackSize;
            ItemStack stack = item.clone();
            stack.setAmount(stackSize);
            itemsToGive.add(stack);
        }

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(itemsToGive.toArray(new ItemStack[0]));

        if (!overflow.isEmpty()) {
            for (ItemStack overflowItem : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
            }
            player.sendMessage("§cYour inventory was full. Some items were dropped at your feet.");
        }
    }

    public void dropArchItem(UUID uuid, int id, Location blockLocation) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = itemCache.get(id);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + id);
            return;
        }
        blockLocation.getWorld().dropItemNaturally(blockLocation, item.clone());
    }

    public String getDisplayName(int itemID) {
        ItemStack item = itemCache.get(itemID);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return "Unknown Item";
    }

    public void reloadItems() {
        itemCache.clear();
        createItemFile();
        loadItems();
    }
}
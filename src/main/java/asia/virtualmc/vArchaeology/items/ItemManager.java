// ItemManager.class
package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.Random;

public class ItemManager {
    private final Main plugin;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    // archItems & archTools maps
    private final Map<Integer, ItemStack> itemCache;
    private final Map<Integer, ItemStack> toolCache;

    // constants for items
    private static final String ITEM_LIST_PATH = "itemsList";
    private static final String NBT_KEY = "VARCH_ITEM";

    // constants for tools
    private static final String TOOLS_LIST_PATH = "toolsList";
    private static final String NBT_TOOL_KEY = "VARCH_TOOL";
    private static final String NBT_GATHER_KEY = "VARCH_GATHER";
    private static final String NBT_ADB_KEY = "VARCH_ADB";

    private final Random random;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        this.itemCache = new ConcurrentHashMap<>();
        this.toolCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "custom-items.yml");
        this.random = new Random();
        createCustomItemsFile();
        loadItems();
        loadTools();
    }

    private void createCustomItemsFile() {
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
                item.setItemMeta(meta);
            }
            NBTItem nbtItem = new NBTItem(item);
            nbtItem.setInteger(NBT_KEY, id);
            return nbtItem.getItem();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create item: " + itemName, e);
            return null;
        }
    }

    private void loadTools() {
        ConfigurationSection toolsList = customItemsConfig.getConfigurationSection(TOOLS_LIST_PATH);
        if (toolsList == null) {
            plugin.getLogger().warning("No tools found in custom-items.yml under 'toolsList'.");
            return;
        }
        for (String toolName : toolsList.getKeys(false)) {
            String path = TOOLS_LIST_PATH + "." + toolName;

            int id = customItemsConfig.getInt(path + ".id", -1);
            String materialName = customItemsConfig.getString(path + ".material");
            String displayName = customItemsConfig.getString(path + ".name");
            int customModelData = customItemsConfig.getInt(path + ".custom-model-data", 0);
            double gatherRate = customItemsConfig.getDouble(path + ".gathering-rate", 0.0);
            double adBonus = customItemsConfig.getDouble(path + ".ad-bonus", 0.0);
            int unbreaking = customItemsConfig.getInt(path + ".unbreaking", 0);
            List<String> lore = customItemsConfig.getStringList(path + ".lore");

            if (id == -1 || materialName == null || displayName == null) {
                plugin.getLogger().warning("Invalid configuration for tool: " + toolName);
                continue;
            }
            ItemStack toolItem = createToolItemInternal(
                    id, materialName, displayName, customModelData,
                    gatherRate, adBonus, unbreaking, lore
            );
            if (toolItem != null) {
                toolCache.put(id, toolItem.clone());
            }
        }
        //plugin.getLogger().info("Loaded tool items into cache: " + toolCache.keySet());
    }

    private ItemStack createToolItemInternal(int id, String materialName, String displayName,
                                             int customModelData, double gatherRate,
                                             double adBonus, int unbreaking, List<String> lore) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                return null;
            }
            meta.setDisplayName(displayName.replace("&", "§"));

            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(line.replace("&", "§"));
            }
            meta.setLore(coloredLore);
            meta.setCustomModelData(customModelData);

            if (unbreaking > 0) {
                meta.addEnchant(Enchantment.UNBREAKING, unbreaking, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            // Add the custom NBT data
            NBTItem nbtTool = new NBTItem(item);
            nbtTool.setInteger(NBT_TOOL_KEY, id);
            nbtTool.setDouble(NBT_GATHER_KEY, gatherRate);
            nbtTool.setDouble(NBT_ADB_KEY, adBonus);

            return nbtTool.getItem();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create tool item with ID " + id, e);
            return null;
        }
    }

    public Integer getItemId(ItemStack item) {
        if (item == null) return null;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_KEY) ? nbtItem.getInteger(NBT_KEY) : null;
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
        ItemStack giveItem = item.clone();
        giveItem.setAmount(Math.min(amount, giveItem.getMaxStackSize()));
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);

        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§eYour inventory was full. Some items were dropped at your feet.");
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

    public Integer getToolId(ItemStack item) {
        if (item == null) return null;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_TOOL_KEY) ? nbtItem.getInteger(NBT_TOOL_KEY) : null;
    }

    public void giveArchTool(UUID uuid, int id) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack toolItem = toolCache.get(id);
        if (toolItem == null) {
            player.sendMessage("§cInvalid tool ID: " + id);
            return;
        }
        ItemStack giveItem = toolItem.clone();
        giveItem.setAmount(Math.min(1, giveItem.getMaxStackSize()));
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);

        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§eYour inventory was full. Some tools were dropped at your feet.");
        }
    }

    public void reloadItems() {
        itemCache.clear();
        customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        loadItems();
        loadItems();
    }
}
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

public class ItemManager {
    private final Main plugin;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    private final Map<Integer, ItemStack> itemCache;
    private final Map<Integer, ItemStack> charmCache;
    private final Map<Integer, ItemStack> toolCache;

    private static final String ITEM_LIST_PATH = "itemsList";
    private static final String CHARM_LIST_PATH = "charmsList";
    private static final String TOOLS_LIST_PATH = "toolsList";

    // NamespacedKeys for PersistentDataContainer
    private final NamespacedKey ITEM_KEY;
    private final NamespacedKey CHARM_KEY;
    private final NamespacedKey TOOL_KEY;
    private final NamespacedKey GATHER_RATE_KEY;
    private final NamespacedKey AD_BONUS_KEY;
    private final NamespacedKey REQ_LEVEL_KEY;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        this.itemCache = new ConcurrentHashMap<>();
        this.charmCache = new ConcurrentHashMap<>();
        this.toolCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "custom-items.yml");

        // Initialize NamespacedKeys
        this.ITEM_KEY = new NamespacedKey(plugin, "varch_item");
        this.CHARM_KEY = new NamespacedKey(plugin, "varch_charm");
        this.TOOL_KEY = new NamespacedKey(plugin, "varch_tool");
        this.GATHER_RATE_KEY = new NamespacedKey(plugin, "varch_gather");
        this.AD_BONUS_KEY = new NamespacedKey(plugin, "varch_adb");
        this.REQ_LEVEL_KEY = new NamespacedKey(plugin, "varch_level");

        createCustomItemsFile();
        loadItems();
        loadCharms();
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

    private void loadCharms() {
        ConfigurationSection itemsList = customItemsConfig.getConfigurationSection(CHARM_LIST_PATH);
        if (itemsList == null) {
            plugin.getLogger().warning("No items found in custom-items.yml");
            return;
        }
        for (String itemName : itemsList.getKeys(false)) {
            String path = CHARM_LIST_PATH + "." + itemName;
            int id = customItemsConfig.getInt(path + ".id", -1);

            if (id == -1) {
                plugin.getLogger().warning("Missing ID for item: " + itemName);
                continue;
            }
            ItemStack item = createArchCharmInternal(itemName, id);
            if (item != null) {
                charmCache.put(id, item.clone());
            }
        }
        plugin.getLogger().info("Loaded items into cache: " + charmCache.keySet());
    }

    private ItemStack createArchCharmInternal(String itemName, int id) {
        try {
            String path = CHARM_LIST_PATH + "." + itemName;
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

                // Set PersistentDataContainer data
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(CHARM_KEY, PersistentDataType.INTEGER, id);

                item.setItemMeta(meta);
            }
            return item;
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
            int reqLevel = customItemsConfig.getInt(path + ".required-level", 1);
            List<String> lore = customItemsConfig.getStringList(path + ".lore");

            if (id == -1 || materialName == null || displayName == null) {
                plugin.getLogger().warning("Invalid configuration for tool: " + toolName);
                continue;
            }
            ItemStack toolItem = createToolItemInternal(
                    id, materialName, displayName, customModelData,
                    gatherRate, adBonus, unbreaking, reqLevel, lore
            );
            if (toolItem != null) {
                toolCache.put(id, toolItem.clone());
            }
        }
    }

    private ItemStack createToolItemInternal(int id, String materialName, String displayName,
                                             int customModelData, double gatherRate,
                                             double adBonus, int unbreaking, int reqLevel, List<String> lore) {
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

            if (unbreaking >= 10) {
                meta.setUnbreakable(true);
            } else if (unbreaking > 0) {
                meta.addEnchant(Enchantment.UNBREAKING, unbreaking, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set PersistentDataContainer data
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(TOOL_KEY, PersistentDataType.INTEGER, id);
            container.set(REQ_LEVEL_KEY, PersistentDataType.INTEGER, reqLevel);
            container.set(GATHER_RATE_KEY, PersistentDataType.DOUBLE, gatherRate);
            container.set(AD_BONUS_KEY, PersistentDataType.DOUBLE, adBonus);

            item.setItemMeta(meta);
            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create tool item with ID " + id, e);
            return null;
        }
    }

    public Integer getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(ITEM_KEY, PersistentDataType.INTEGER);
    }

    public Integer getToolId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(TOOL_KEY, PersistentDataType.INTEGER);
    }

    public Integer getRequiredLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(REQ_LEVEL_KEY, PersistentDataType.INTEGER);
    }

    public Double getGatherRate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(GATHER_RATE_KEY, PersistentDataType.DOUBLE);
    }

    public Double getAdBonus(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(AD_BONUS_KEY, PersistentDataType.DOUBLE);
    }

    public boolean isArchTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(TOOL_KEY, PersistentDataType.INTEGER);
    }

    public int getCharmID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(CHARM_KEY, PersistentDataType.INTEGER, 0);
    }

    public Integer getDurability(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable)) {
            return null;
        }
        org.bukkit.inventory.meta.Damageable itemMeta = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
        int maxDurability = item.getType().getMaxDurability();

        if (maxDurability == 0) {
            return null;
        }
        return maxDurability - itemMeta.getDamage();
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
        giveItem.setAmount(1);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);

        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§eYour inventory was full. Some tools were dropped at your feet.");
        }
    }

    public void giveArchCharm(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = charmCache.get(id);
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

    public void reloadItems() {
        itemCache.clear();
        toolCache.clear();
        customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        loadItems();
        loadTools();
    }
}
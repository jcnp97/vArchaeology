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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CustomTools {
    private final Main plugin;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    private final Map<Integer, ItemStack> toolCache;
    private static final String TOOLS_LIST_PATH = "toolsList";
    private final NamespacedKey TOOL_KEY;
    private final NamespacedKey GATHER_RATE_KEY;
    private final NamespacedKey AD_BONUS_KEY;
    private final NamespacedKey REQ_LEVEL_KEY;

    public CustomTools(Main plugin) {
        this.plugin = plugin;
        this.toolCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "items/tools.yml");

        this.TOOL_KEY = new NamespacedKey(plugin, "varch_tool");
        this.GATHER_RATE_KEY = new NamespacedKey(plugin, "varch_gather");
        this.AD_BONUS_KEY = new NamespacedKey(plugin, "varch_adb");
        this.REQ_LEVEL_KEY = new NamespacedKey(plugin, "varch_level");

        createToolFile();
        loadTools();
    }

    private void createToolFile() {
        try {
            if (!customItemsFile.exists()) {
                customItemsFile.getParentFile().mkdirs();
                plugin.saveResource("items/tools.yml", false);
            }
            customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to create tools file: ", e);
        }
    }

    private void loadTools() {
        ConfigurationSection toolsList = customItemsConfig.getConfigurationSection(TOOLS_LIST_PATH);
        if (toolsList == null) {
            plugin.getLogger().warning("No tools found in items/tools.yml under 'toolsList'.");
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

    public ItemStack getArchToolCache(int id) {
        return toolCache.get(id);
    }

    public int getToolModelData(int itemID) {
        ItemStack item = toolCache.get(itemID);
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return 0;
        }
        return meta.getCustomModelData();
    }

    public String getDisplayName(int toolID) {
        ItemStack item = toolCache.get(toolID);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return "Unknown Item";
    }

    public String getMaterialName(int toolID) {
        ItemStack item = toolCache.get(toolID);
        if (item != null) {
            return item.getType().name();
        }
        return Material.AIR.name();
    }

    public void reloadTools() {
        toolCache.clear();
        createToolFile();
        loadTools();
    }


}
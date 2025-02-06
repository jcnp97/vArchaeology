package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CustomAugments {
    private final Main plugin;
    private final File customItemsFile;
    private FileConfiguration customItemsConfig;

    private final Map<Integer, ItemStack> augmentCache;
    private static final String AUGMENTS_PATH_LIST = "augmentsList";
    private final NamespacedKey AUGMENT_KEY;

    public CustomAugments(Main plugin) {
        this.plugin = plugin;
        this.augmentCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "items/augments.yml");
        this.AUGMENT_KEY = new NamespacedKey(plugin, "varch_augment");

        createCharmFile();
        loadCharms();
    }

    private void createCharmFile() {
        try {
            if (!customItemsFile.exists()) {
                customItemsFile.getParentFile().mkdirs();
                plugin.saveResource("items/augments.yml", false);
            }
            customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to create augments item file: ", e);
        }
    }

    private void loadCharms() {
        try {
            ConfigurationSection itemsList = customItemsConfig.getConfigurationSection(AUGMENTS_PATH_LIST);
            if (itemsList == null) {
                plugin.getLogger().warning("No items found in items/augments.yml");
                return;
            }
            for (String itemName : itemsList.getKeys(false)) {
                String path = AUGMENTS_PATH_LIST + "." + itemName;
                int id = customItemsConfig.getInt(path + ".id", -1);

                if (id == -1) {
                    plugin.getLogger().warning("Missing ID for augment item: " + itemName);
                    continue;
                }
                ItemStack item = createCharms(itemName, id);
                if (item != null) {
                    augmentCache.put(id, item.clone());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to load into cache: " + augmentCache.keySet());
        }
    }

    private ItemStack createCharms(String itemName, int id) {
        try {
            String path = AUGMENTS_PATH_LIST + "." + itemName;
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
                container.set(AUGMENT_KEY, PersistentDataType.INTEGER, id);

                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create item: " + itemName, e);
            return null;
        }
    }

    public int getCharmID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(AUGMENT_KEY, PersistentDataType.INTEGER, 0);
    }

    public void giveCharm(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = augmentCache.get(id);
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

    public void giveAllAugments(UUID uuid, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        for (int i = 0; i < augmentCache.size(); i++) {
            ItemStack item = augmentCache.get(i + 1);
            if (item == null) {
                player.sendMessage("§cInvalid item ID: " + i);
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
    }

    public void reloadCharms() {
        augmentCache.clear();
        createCharmFile();
        loadCharms();
    }
}
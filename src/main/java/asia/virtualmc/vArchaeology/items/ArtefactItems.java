package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.utilities.EffectsUtil;
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

public class ArtefactItems {
    private final Main plugin;
    private final File customItemsFile;
    private final EffectsUtil effectsUtil;
    private FileConfiguration customItemsConfig;
    private Random random;

    private final Map<Integer, ItemStack> artefactCache;
    private static final String ARTEFACT_LIST_PATH = "artefactsList";
    private final NamespacedKey ARTEFACT_KEY;
    private final NamespacedKey UNSTACKABLE_KEY;

    public ArtefactItems(Main plugin, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.artefactCache = new ConcurrentHashMap<>();
        this.customItemsFile = new File(plugin.getDataFolder(), "items/artefacts.yml");
        this.ARTEFACT_KEY = new NamespacedKey(plugin, "varch_artefact");
        this.UNSTACKABLE_KEY = new NamespacedKey(plugin, "varch_unique_id");
        this.random = new Random();
        this.effectsUtil = effectsUtil;

        createArtefactFile();
        loadArtefacts();
    }

    private void createArtefactFile() {
        try {
            if (!customItemsFile.exists()) {
                customItemsFile.getParentFile().mkdirs();
                plugin.saveResource("items/artefacts.yml", false);
            }
            customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to create artefact item file: ", e);
        }
    }

    private void loadArtefacts() {
        try {
            ConfigurationSection itemsList = customItemsConfig.getConfigurationSection(ARTEFACT_LIST_PATH);
            if (itemsList == null) {
                plugin.getLogger().warning("No items found in items/artefacts.yml");
                return;
            }
            for (String itemName : itemsList.getKeys(false)) {
                String path = ARTEFACT_LIST_PATH + "." + itemName;
                int id = customItemsConfig.getInt(path + ".id", -1);

                if (id == -1) {
                    plugin.getLogger().warning("Missing ID for item: " + itemName);
                    continue;
                }
                ItemStack item = createArtefacts(itemName, id);
                if (item != null) {
                    artefactCache.put(id, item.clone());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to load item into cache: " + artefactCache.keySet());
        }
    }

    private ItemStack createArtefacts(String itemName, int id) {
        try {
            String path = ARTEFACT_LIST_PATH + "." + itemName;
            String materialName = customItemsConfig.getString(path + ".material");
            String name = customItemsConfig.getString(path + ".name");
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

                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(ARTEFACT_KEY, PersistentDataType.INTEGER, id);
                int randomValue = (int) (Math.random() * Integer.MAX_VALUE);
                container.set(UNSTACKABLE_KEY, PersistentDataType.INTEGER, randomValue);

                item.setItemMeta(meta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create item: " + itemName, e);
            return null;
        }
    }

    public String getDisplayName(int id) {
        ItemStack item = artefactCache.get(id);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return "Unknown Item"; // Default fallback if the item is not found or has no name
    }

    public int getArtefactID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(ARTEFACT_KEY, PersistentDataType.INTEGER, 0);
    }

    public void giveArtefact(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = artefactCache.get(id);
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
            player.sendMessage("§cYour inventory was full. Some items were dropped at your feet.");
        }
    }

    public void giveRandomArtefact(UUID uuid, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        effectsUtil.sendPlayerMessage(uuid, "<#00FFA2>You have discovered an unidentified Artefact.");
        int artefactID = random.nextInt(8) + 1;
        if (player == null) {
            return;
        }
        ItemStack item = artefactCache.get(artefactID);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + artefactID);
            return;
        }
        ItemStack giveItem = item.clone();
        giveItem.setAmount(Math.min(amount, giveItem.getMaxStackSize()));
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);

        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§cYour inventory was full. Some items were dropped at your feet.");
        }
    }

    public void reloadArtefactItems() {
        artefactCache.clear();
        createArtefactFile();
        loadArtefacts();
    }
}
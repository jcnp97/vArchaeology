package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ArtefactCollections {
    private final Main plugin;
    private final File collectionsFile;
    private final EffectsUtil effectsUtil;
    private final CollectionLog collectionLog;
    private final ConfigManager configManager;
    private final Random random;
    private FileConfiguration collectionsConfig;
    private final Map<Integer, ItemStack> collectionCache;
    private final Map<Integer, String> modelDataToFileName;
    private boolean generateModels;
    private final NamespacedKey COLLECTION_KEY;
    private final NamespacedKey UNSTACKABLE_KEY;

    public ArtefactCollections(Main plugin,
                               EffectsUtil effectsUtil,
                               CollectionLog collectionLog,
                               ConfigManager configManager) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.collectionLog = collectionLog;
        this.configManager = configManager;
        this.random = new Random();
        this.collectionCache = new ConcurrentHashMap<>();
        this.modelDataToFileName = new HashMap<>();
        this.collectionsFile = new File(plugin.getDataFolder(), "items/collections.yml");
        this.COLLECTION_KEY = new NamespacedKey(plugin, "varch_collection");
        this.UNSTACKABLE_KEY = new NamespacedKey(plugin, "varch_unique_id");

        createCollectionsFile();
        loadCollections();

        this.generateModels = YamlConfiguration.loadConfiguration(collectionsFile).getBoolean("globalSettings.generate-models", false);
        if (generateModels) {
            generateModelJsonFiles();
            generateFlintJson();
        }
    }

    private void createCollectionsFile() {
        try {
            if (!collectionsFile.exists()) {
                collectionsFile.getParentFile().mkdirs();
                plugin.saveResource("items/collections.yml", false);
            }
            this.collectionsConfig = YamlConfiguration.loadConfiguration(collectionsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[vArchaeology] Failed to create/load collections.yml: ", e);
        }
    }

    private void loadCollections() {
        if (collectionsConfig == null) {
            plugin.getLogger().warning("No collectionsConfig found. Check if collections.yml is loaded properly.");
            return;
        }

        ConfigurationSection collectionListSection = collectionsConfig.getConfigurationSection("collectionList");
        if (collectionListSection == null) {
            plugin.getLogger().warning("No 'collectionList' section found in collections.yml");
            return;
        }

        int currentItemId = 1;

        for (String groupId : collectionListSection.getKeys(false)) {
            ConfigurationSection groupSection = collectionListSection.getConfigurationSection(groupId);
            if (groupSection == null) {
                continue;
            }

            String materialName = groupSection.getString("material", "STONE");
            List<String> lore = groupSection.getStringList("lore");
            List<String> itemNames = groupSection.getStringList("collections");

            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' in group " + groupId + ". Using STONE as fallback.");
                material = Material.STONE;
            }

            for (String collectionItemName : itemNames) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(("§4" + collectionItemName).replace("&", "§"));

                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(line.replace("&", "§"));
                    }
                    meta.setLore(coloredLore);

                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    container.set(COLLECTION_KEY, PersistentDataType.INTEGER, currentItemId);

                    int cmd = configManager.startingModelData + (currentItemId - 1);
                    meta.setCustomModelData(cmd);

                    item.setItemMeta(meta);

                    String convertedName = convertCollectionName(collectionItemName);
                    modelDataToFileName.put(cmd, convertedName);
                }

                collectionCache.put(currentItemId, item);
                currentItemId++;
            }
        }

        plugin.getLogger().info("[vArchaeology] Loaded " + collectionCache.size() + " artefact items from collections.yml");
    }

    public int getCollectionID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(COLLECTION_KEY, PersistentDataType.INTEGER, 0);
    }

    public void giveCollection(UUID uuid, int id, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        ItemStack item = collectionCache.get(id);
        if (item == null) {
            player.sendMessage("§cInvalid item ID: " + id);
            return;
        }
        ItemStack giveItem = item.clone();
        ItemMeta meta = giveItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(UNSTACKABLE_KEY, PersistentDataType.INTEGER, random.nextInt(Integer.MAX_VALUE));
        giveItem.setItemMeta(meta);
        giveItem.setAmount(Math.min(amount, giveItem.getMaxStackSize()));

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(giveItem);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
            player.sendMessage("§cYour inventory was full. Some items were dropped at your feet.");
        }
        collectionLog.incrementCollection(uuid, id + 7);
    }

    public void reloadArtefactItems() {
        collectionCache.clear();
        createCollectionsFile();
        loadCollections();
    }

    /**
     *
     * MODEL GENERATOR
     *
     */
    private String convertCollectionName(String rawName) {
        // Lowercase
        String name = rawName.toLowerCase();
        // Replace any apostrophes or punctuation with nothing
        name = name.replaceAll("[^a-z0-9\\s_]", "");
        // Replace whitespace with underscore
        name = name.replaceAll("\\s+", "_");
        return name;
    }

    public void generateModelJsonFiles() {
        File baseFolder = new File(plugin.getDataFolder(), "generated/models/cozyvanilla/item/archaeology_collection");
        if (!baseFolder.exists() && !baseFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create folder: " + baseFolder.getAbsolutePath());
            return;
        }

        for (Map.Entry<Integer, String> entry : modelDataToFileName.entrySet()) {
            String fileName = entry.getValue(); // e.g. "centurions_sword"
            File modelFile = new File(baseFolder, fileName + ".json");

            String modelJson = "{\n" +
                    "  \"parent\": \"minecraft:item/generated\",\n" +
                    "  \"textures\": {\n" +
                    "    \"layer0\": \"cozyvanilla:item/archaeology_collection/" + fileName + "\"\n" +
                    "  }\n" +
                    "}";

            try (FileWriter writer = new FileWriter(modelFile, StandardCharsets.UTF_8)) {
                writer.write(modelJson);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to write model JSON file: " + modelFile.getName(), e);
            }
        }

        plugin.getLogger().info("[vArchaeology] Generated " + modelDataToFileName.size() + " model JSON files.");
    }

    public void generateFlintJson() {
        File baseFolder = new File(plugin.getDataFolder(), "generated/models/item");
        if (!baseFolder.exists() && !baseFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create folder: " + baseFolder.getAbsolutePath());
            return;
        }

        File flintFile = new File(baseFolder, "flint.json");
        StringBuilder overridesBuilder = new StringBuilder();
        overridesBuilder.append("  \"overrides\": [\n");

        List<Integer> sortedCmds = new ArrayList<>(modelDataToFileName.keySet());
        Collections.sort(sortedCmds);

        for (int i = 0; i < sortedCmds.size(); i++) {
            int cmd = sortedCmds.get(i);
            String fileName = modelDataToFileName.get(cmd);

            overridesBuilder.append("    {\n");
            overridesBuilder.append("      \"predicate\": {\n");
            overridesBuilder.append("        \"custom_model_data\": ").append(cmd).append("\n");
            overridesBuilder.append("      },\n");
            overridesBuilder.append("      \"model\": \"cozyvanilla:item/archaeology_collection/").append(fileName).append("\"\n");
            overridesBuilder.append("    }");

            if (i < sortedCmds.size() - 1) {
                overridesBuilder.append(",");
            }
            overridesBuilder.append("\n");
        }
        overridesBuilder.append("  ]\n");

        String flintJson = "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"item/flint\"\n" +
                "  },\n" +
                overridesBuilder +
                "}";

        try (FileWriter writer = new FileWriter(flintFile, StandardCharsets.UTF_8)) {
            writer.write(flintJson);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write flint.json", e);
        }

        plugin.getLogger().info("[vArchaeology] Generated flint.json with " + modelDataToFileName.size() + " overrides.");
    }

    public int getCollectionModelData(int itemId) {
        ItemStack item = collectionCache.get(itemId);
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return 0;
        }
        return meta.getCustomModelData();
    }
}

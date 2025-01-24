package asia.virtualmc.vArchaeology.configs;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private Main plugin;
    // database.yml
    public String host;
    public String dbname;
    public String username;
    public String password;
    public int port;
    // experience-table.yml
    public final Map<Integer, Integer> experienceTable;
    // custom-items.yml
    public int commonWeight;
    public int uncommonWeight;
    public int rareWeight;
    public int uniqueWeight;
    public int specialWeight;
    public int mythicalWeight;
    public int exoticWeight;
    // guis.yml
    public String salvageMenu;
    public String compMenu;
    public String traitMenu;
    public String traitUp;
    public String salvageSound;
    public String confirmMaterial;
    public int confirmModelData;
    public String cancelMaterial;
    public int cancelModelData;
    // others
    public String pluginPrefix = "<#0040FF>[vArch<#00FBFF>aeology] ";

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.experienceTable = new HashMap<>();
        readConfigs();
    }

    public void readConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        // Read and load YAML files into variables
        readDatabase();
        readExperienceTable();
        readcustomDrops();
        readGUISettings();
        blocksEXPConfig();
    }

    public void readDatabase() {
        File dbConfigFile = new File(plugin.getDataFolder(), "database.yml");
        if (!dbConfigFile.exists()) {
            try {
                plugin.saveResource("database.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration dbConfig = YamlConfiguration.loadConfiguration(dbConfigFile);
        try {
            host = dbConfig.getString("mysql.host", "localhost");
            port = dbConfig.getInt("mysql.port", 3306);
            dbname = dbConfig.getString("mysql.database", "minecraft");
            username = dbConfig.getString("mysql.username", "root");
            password = dbConfig.getString("mysql.password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readExperienceTable() {
        File expTableFile = new File(plugin.getDataFolder(), "experience-table.yml");
        if (!expTableFile.exists()) {
            try {
                plugin.saveResource("experience-table.yml", false);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[vArchaeology] Experience table not found. Disabling the plugin..");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(expTableFile);
        int previousExp = -1;
        experienceTable.clear();
        for (String key : config.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String expString = config.getString(key, "0").replace(",", "");
                int exp = Integer.parseInt(expString);

                if (previousExp >= 0 && exp <= previousExp) {
                    throw new IllegalStateException("[vArchaeology] Invalid progression: Level " + level +
                            " has lower or equal EXP than previous level");
                }
                experienceTable.put(level, exp);
                previousExp = exp;
            } catch (NumberFormatException e) {
                plugin.getLogger().severe("[vArchaeology] Invalid number format in experience table at level " + key);
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            } catch (IllegalStateException e) {
                plugin.getLogger().severe(e.getMessage());
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Experience table has been loaded.");
    }

    public void readcustomDrops() {
        File dropsFile = new File(plugin.getDataFolder(), "custom-items.yml");
        if (!dropsFile.exists()) {
            try {
                plugin.saveResource("custom-items.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration drops = YamlConfiguration.loadConfiguration(dropsFile);
        try {
            commonWeight = drops.getInt("itemsList.purpleheart_wood.weight", 55);
            uncommonWeight = drops.getInt("itemsList.imperial_steel.weight", 35);
            rareWeight = drops.getInt("itemsList.everlight_silvthril.weight", 25);
            uniqueWeight = drops.getInt("itemsList.chaotic_brimstone.weight", 15);
            specialWeight = drops.getInt("itemsList.hellfire_metal.weight", 8);
            mythicalWeight = drops.getInt("itemsList.aetherium_alloy.weight", 4);
            exoticWeight = drops.getInt("itemsList.quintessence.weight", 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readGUISettings() {
        File dropsFile = new File(plugin.getDataFolder(), "guis.yml");
        if (!dropsFile.exists()) {
            try {
                plugin.saveResource("guis.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration gui = YamlConfiguration.loadConfiguration(dropsFile);
        try {
            confirmMaterial = gui.getString("guiSettings.confirm-material", "EMERALD");
            cancelMaterial = gui.getString("guiSettings.cancel-material", "REDSTONE_BLOCK");
            confirmModelData = gui.getInt("guiSettings.confirm-model_data", 1);
            cancelModelData = gui.getInt("guiSettings.cancel-model_data", 1);
            salvageMenu = gui.getString("guiSettings.salvage_station.menuTitle", "Salvage GUI");
            compMenu = gui.getString("guiSettings.salvage_station.menuTitle-components", "Components GUI");
            traitMenu = gui.getString("guiSettings.traitGUI.menuTitle", "Traits GUI");
            traitUp = gui.getString("guiSettings.traitGUI.upgradeTitle", "Traits GUI");
            salvageSound = gui.getString("guiSettings.salvage_station.soundOnClick", "minecraft:block.anvil.use");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void blocksEXPConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("settings.blocksList.SAND")) {
            config.set("settings.blocksList.SAND", 1);
        }

        if (!config.contains("settings.blocksList.GRAVEL")) {
            config.set("settings.blocksList.GRAVEL", 1);
        }

        if (!config.contains("settings.blocksList.GRASS_BLOCK")) {
            config.set("settings.blocksList.GRASS_BLOCK", 1);
        }

        if (!config.contains("settings.blocksList.DIRT")) {
            config.set("settings.blocksList.DIRT", 1);
        }

        if (!config.contains("settings.blocksList.CLAY")) {
            config.set("settings.blocksList.CLAY", 1);
        }
        plugin.saveConfig();
    }

    public List<String> loadTalentNames() {
        File talentFile = new File(plugin.getDataFolder(), "talent-trees.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(talentFile);
        List<String> talentNames = new ArrayList<>();

        if (config.isConfigurationSection("talentList")) {
            for (String key : config.getConfigurationSection("talentList").getKeys(false)) {
                String name = config.getString("talentList." + key + ".name");
                if (name != null) {
                    talentNames.add(name);
                }
            }
        }
        return talentNames;
    }

    public List<String> loadCollections() {
        File collectionFile = new File(plugin.getDataFolder(), "collection-log.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(collectionFile);
        List<String> collectionList = new ArrayList<>();

        if (config.isList("collectionList")) {
            collectionList.addAll(config.getStringList("collectionList"));
        }
        return collectionList;
    }

    public Map<Material, Integer> loadBlocksList() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection blocksSection = config.getConfigurationSection("settings.blocksList");
        Map<Material, Integer> blocksList = new HashMap<>();

        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    int expValue = blocksSection.getInt(key);
                    blocksList.put(material, expValue);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + key);
                }
            }
        }
        return blocksList;
    }


}

package asia.virtualmc.vArchaeology.configs;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private Main plugin;
    // database.yml
    public String host;
    public String dbname;
    public String username;
    public String password;
    public int port;
    // experience-table.yml
    public final Map<Integer, Integer> experienceTable = new HashMap<>();
    // items.yml
    public int[] dropWeights = {0, 0, 0, 0, 0, 0, 0};
    public double[] dropBasePrice = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    // guis.yml
    public int invisibleModelData;
    public String salvageGUITitle;
    public String salvageGUIComp;
    public String traitGUITitle;
    public String traitGUIUpgrade;
    public String rankGUITitle;
    public String sellGUITitle;
    public String confirmGUITitle;
    public String arteRestoreGUITitle;
    // traits.yml
    public double[] wisdomEffects = {0.0, 0.0, 0.0, 0.0};
    public double[] charismaEffects = {0.0, 0.0, 0.0, 0.0};
    public double[] karmaEffects = {0.0, 0.0, 0.0, 0.0};
    public double[] dexterityEffects = {0.0, 0.0, 0.0, 0.0};
    // ranks.yml
    public final Map<Integer, rankInfo> rankTable = new HashMap<>();
    public record rankInfo(int pointsRequired, String rankName) {}
    // others
    public String pluginPrefix = "<#0040FF>[vArch<#00FBFF>aeology] ";
    public boolean configDebug;
    public int startingModelData;
    // collection-log.yml
    public final Map<Integer, List<String>> rarityLore = new HashMap<>();
    public final Map<Integer, List<String>> groupLore = new HashMap<>();
    public String acquiredLore;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        readConfigs();
    }

    public void readConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        // Read and load YAML files into variables
        readSettings();
        readDatabase();
        readExperienceTable();
        readcustomDrops();
        readGUISettings();
        readTraitSettings();
        readRanks();
        readCollectionLore();
        // Read variable
        startingModelData = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "items/collections.yml")).getInt("globalSettings.starting-model-data", 1);
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
        File dropsFile = new File(plugin.getDataFolder(), "items/items.yml");
        if (!dropsFile.exists()) {
            try {
                plugin.saveResource("items/items.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration drops = YamlConfiguration.loadConfiguration(dropsFile);
        try {
            dropWeights[0] = drops.getInt("itemsList.purpleheart_wood.weight", 55);
            dropWeights[1] = drops.getInt("itemsList.imperial_steel.weight", 35);
            dropWeights[2] = drops.getInt("itemsList.everlight_silvthril.weight", 25);
            dropWeights[3] = drops.getInt("itemsList.chaotic_brimstone.weight", 15);
            dropWeights[4] = drops.getInt("itemsList.hellfire_metal.weight", 8);
            dropWeights[5] = drops.getInt("itemsList.aetherium_alloy.weight", 4);
            dropWeights[6] = drops.getInt("itemsList.quintessence.weight", 1);

            if (configDebug) {
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Archaeology drops loaded: "
                        + dropWeights[0] + ", " + dropWeights[1] + ", " + dropWeights[2] + ", " + dropWeights[3] + ", "
                        + dropWeights[4] + ", " + dropWeights[5] + ", " + dropWeights[6]
                );
            }

            dropBasePrice[0] = drops.getDouble("itemsList.purpleheart_wood.sell-price", 0);
            dropBasePrice[1] = drops.getDouble("itemsList.imperial_steel.sell-price", 0);
            dropBasePrice[2] = drops.getDouble("itemsList.everlight_silvthril.sell-price", 0);
            dropBasePrice[3] = drops.getDouble("itemsList.chaotic_brimstone.sell-price", 0);
            dropBasePrice[4] = drops.getDouble("itemsList.hellfire_metal.sell-price", 0);
            dropBasePrice[5] = drops.getDouble("itemsList.aetherium_alloy.sell-price", 0);
            dropBasePrice[6] = drops.getDouble("itemsList.quintessence.sell-price", 0);

            if (configDebug) {
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Archaeology drop prices loaded: "
                        + dropBasePrice[0] + ", " + dropBasePrice[1] + ", " + dropBasePrice[2] + ", " + dropBasePrice[3] + ", "
                        + dropBasePrice[4] + ", " + dropBasePrice[5] + ", " + dropBasePrice[6]
                );
            }
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
            invisibleModelData = gui.getInt("guiSettings.invisible-model_data", 1);
            salvageGUITitle = gui.getString("guiSettings.salvagegui-title", "Salvage GUI");
            salvageGUIComp = gui.getString("guiSettings.salvagegui-comp", "Components GUI");
            traitGUITitle = gui.getString("guiSettings.traitgui-title", "Traits GUI");
            traitGUIUpgrade = gui.getString("guiSettings.traitgui-upgrade", "Traits Upgrade");
            rankGUITitle = gui.getString("guiSettings.rankgui-title", "Rank GUI");
            sellGUITitle = gui.getString("guiSettings.sellgui-title", "Sell GUI");
            confirmGUITitle = gui.getString("guiSettings.confirmgui-title", "Are you sure?");
            arteRestoreGUITitle = gui.getString("guiSettings.artefact-restoregui-title", "Restore Artefacts GUI");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        ConfigurationSection collectionSection = config.getConfigurationSection("collectionList");
        if (collectionSection == null) {
            return collectionList;
        }

        if (collectionSection.isList("ungrouped")) {
            collectionList.addAll(collectionSection.getStringList("ungrouped"));
        }

        ConfigurationSection groupedSection = collectionSection.getConfigurationSection("grouped");
        if (groupedSection != null) {
            List<Integer> sortedKeys = new ArrayList<>();
            for (String key : groupedSection.getKeys(false)) {
                try {
                    sortedKeys.add(Integer.parseInt(key));
                } catch (NumberFormatException e) {
                }
            }
            Collections.sort(sortedKeys);
            for (Integer key : sortedKeys) {
                String keyStr = String.valueOf(key);
                if (groupedSection.isList(keyStr)) {
                    collectionList.addAll(groupedSection.getStringList(keyStr));
                }
            }
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

    public void readTraitSettings() {
        File dropsFile = new File(plugin.getDataFolder(), "traits.yml");
        if (!dropsFile.exists()) {
            try {
                plugin.saveResource("traits.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration trait = YamlConfiguration.loadConfiguration(dropsFile);
        try {
            wisdomEffects[0] = trait.getDouble("traitList.wisdom.effects.block-break", 0.0);
            wisdomEffects[1] = trait.getDouble("traitList.wisdom.effects.receive-material", 0.0);
            wisdomEffects[2] = trait.getDouble("traitList.wisdom.effects.artefact-restoration", 0.0);
            wisdomEffects[3] = trait.getDouble("traitList.wisdom.effects.max-trait-bonus", 0.0);

            charismaEffects[0] = trait.getDouble("traitList.charisma.effects.archaeology-drops", 0.0);
            charismaEffects[1] = trait.getDouble("traitList.charisma.effects.artefacts", 0.0);
            charismaEffects[2] = trait.getDouble("traitList.charisma.effects.aptitude-gain", 0.0);
            charismaEffects[3] = trait.getDouble("traitList.charisma.effects.max-trait-bonus", 0.0);

            karmaEffects[0] = trait.getDouble("traitList.karma.effects.gathering-rate", 0.0);
            karmaEffects[1] = trait.getDouble("traitList.karma.effects.extra-roll", 0.0);
            karmaEffects[2] = trait.getDouble("traitList.karma.effects.next-tier-roll", 0.0);
            karmaEffects[3] = trait.getDouble("traitList.karma.effects.max-trait-bonus", 0.0);

            dexterityEffects[0] = trait.getDouble("traitList.dexterity.effects.artefact-discovery-progress", 0.0);
            dexterityEffects[1] = trait.getDouble("traitList.dexterity.effects.double-adp", 0.0);
            dexterityEffects[2] = trait.getDouble("traitList.dexterity.effects.gain-adp", 0.0);
            dexterityEffects[3] = trait.getDouble("traitList.dexterity.effects.max-trait-bonus", 0.0);

            if (configDebug) {
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Wisdom traits loaded: "
                + wisdomEffects[0] + ", " + wisdomEffects[1] + ", " + wisdomEffects[2] + ", " + wisdomEffects[3]);
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Charisma traits loaded: "
                        + charismaEffects[0] + ", " + charismaEffects[1] + ", " + charismaEffects[2] + ", " + charismaEffects[3]);
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Karma traits loaded: "
                        + karmaEffects[0] + ", " + karmaEffects[1] + ", " + karmaEffects[2] + ", " + karmaEffects[3]);
                ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Dexterity traits loaded: "
                        + dexterityEffects[0] + ", " + dexterityEffects[1] + ", " + dexterityEffects[2] + ", " + dexterityEffects[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readSettings() {
        FileConfiguration config = plugin.getConfig();
        try {
            configDebug = config.getBoolean("settings.config-debug", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readRanks() {
        File rankTableFile = new File(plugin.getDataFolder(), "ranks.yml");
        if (!rankTableFile.exists()) {
            try {
                plugin.saveResource("ranks.yml", false);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[vArchaeology] Rank table not found.");
            }
        }
        FileConfiguration ranksConfig = YamlConfiguration.loadConfiguration(rankTableFile);
        int prevPoints = -1;

        for (String key : ranksConfig.getKeys(false)) {
            try {
                int rankLevel = Integer.parseInt(key);
                int points = ranksConfig.getInt(key + ".points");
                String rankName = ranksConfig.getString(key + ".rankName");

                if (prevPoints >= 0 && points <= prevPoints) {
                    throw new IllegalStateException("[vArchaeology] Invalid progression: Rank " + rankLevel +
                            " has lower or equal EXP than previous level");
                }
                rankTable.put(rankLevel, new rankInfo(points, rankName));
                prevPoints = points;
            } catch (NumberFormatException e) {
                plugin.getLogger().severe("[vArchaeology] Invalid number format in rank table at level " + key);
                return;
            } catch (IllegalStateException e) {
                plugin.getLogger().severe(e.getMessage());
                return;
            }
        }
        ConsoleMessageUtil.sendConsoleMessage(pluginPrefix + "<#7CFEA7>Rank table has been loaded.");
    }

    private void readCollectionLore() {
        File collectionFile = new File(plugin.getDataFolder(), "collection-log.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(collectionFile);

        ConfigurationSection raritySection = config.getConfigurationSection("globalSettings.by-rarity-lore");
        if (raritySection != null) {
            for (String key : raritySection.getKeys(false)) {
                try {
                    int rarityKey = Integer.parseInt(key);
                    List<String> loreList = raritySection.getStringList(key);
                    rarityLore.put(rarityKey, loreList);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        ConfigurationSection groupSection = config.getConfigurationSection("globalSettings.by-group-lore");
        if (groupSection != null) {
            for (String key : groupSection.getKeys(false)) {
                try {
                    int groupKey = Integer.parseInt(key);
                    List<String> loreList = groupSection.getStringList(key);
                    groupLore.put(groupKey, loreList);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        acquiredLore = config.getString("globalSettings.acquired-lore");
    }
}

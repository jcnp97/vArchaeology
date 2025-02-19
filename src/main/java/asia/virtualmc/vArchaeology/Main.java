package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.blocks.CraftingStation;
import asia.virtualmc.vArchaeology.blocks.RestorationStation;
import asia.virtualmc.vArchaeology.commands.BlockCommands;
import asia.virtualmc.vArchaeology.commands.GUICommands;
import asia.virtualmc.vArchaeology.commands.PlayerDataCommands;
import asia.virtualmc.vArchaeology.commands.ItemCommands;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.droptables.ItemsDropTable;
import asia.virtualmc.vArchaeology.guis.*;
import asia.virtualmc.vArchaeology.items.*;
import asia.virtualmc.vArchaeology.listeners.*;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.logs.CraftingLog;
import asia.virtualmc.vArchaeology.logs.LogManager;
import asia.virtualmc.vArchaeology.logs.SalvageLog;
import asia.virtualmc.vArchaeology.logs.SellLog;
import asia.virtualmc.vArchaeology.placeholders.PlaceholderPlayerData;
import asia.virtualmc.vArchaeology.storage.*;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Main extends JavaPlugin {
    // storage
    private PlayerData playerData;
    private Statistics statistics;
    private PlayerDataDB playerDataDB;
    private TalentTree talentTree;
    private CollectionLog collectionLog;
    // items
    private CustomTools customTools;
    private CustomCharms customCharms;
    private CustomItems customItems;
    private MiscItems miscItems;
    private ArtefactItems artefactItems;
    private ItemCommands itemCommands;
    private ArtefactCollections artefactCollections;
    private CraftingMaterials craftingMaterials;
    private CustomAugments customAugments;
    // utils
    private BossBarUtil bossBarUtil;
    private EffectsUtil effectsUtil;
    // droptables
    private ItemsDropTable itemsDropTable;
    // configs
    private ConfigManager configManager;
    // listeners
    private MiscListener miscListener;
    private BlockBreakListener blockBreakListener;
    private PlayerJoinListener playerJoinListener;
    private ItemEquipListener itemEquipListener;
    private ToolRestrictionListener toolRestrictionListener;
    private ItemInteractListener itemInteractListener;
    // blocks
    private RestorationStation restorationStation;
    private CraftingStation craftingStation;
    // exp
    private EXPManager expManager;
    // guis
    private SellGUI sellGUI;
    private TalentGUI talentGUI;
    private SalvageGUI salvageGUI;
    private TraitGUI traitGUI;
    private LampStarGUI lampStarGUI;
    private RankGUI rankGUI;
    private CollectionLogGUI collectionLogGUI;
    private CollectionsGUI collectionsGUI;
    // logs
    private LogManager logManager;
    private SalvageLog salvageLog;
    private SellLog sellLog;
    private CraftingLog craftingLog;
    // commands
    private PlayerDataCommands playerDataCommands;
    private GUICommands guiCommands;
    private BlockCommands blockCommands;
    // placeholders
    private PlaceholderPlayerData placeholderPlayerData;

    private static Economy econ = null;
    private static Permission perms = null;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.configManager = new ConfigManager(this);
        this.bossBarUtil = new BossBarUtil(this);
        this.effectsUtil = new EffectsUtil(this);
        this.customCharms = new CustomCharms(this);
        this.customItems = new CustomItems(this);
        this.miscItems = new MiscItems(this);
        this.customAugments = new CustomAugments(this);
        this.artefactItems = new ArtefactItems(this, effectsUtil);
        this.miscListener = new MiscListener(this);
        this.toolRestrictionListener = new ToolRestrictionListener(this);
        this.logManager = new LogManager(this);
        this.salvageLog = new SalvageLog(this, logManager);
        this.sellLog = new SellLog(this, logManager);
        this.craftingLog = new CraftingLog(this, logManager);
        this.customTools = new CustomTools(this, craftingLog);
        this.craftingMaterials = new CraftingMaterials(this, craftingLog);
        this.playerDataDB = new PlayerDataDB(this, configManager);
        this.statistics = new Statistics(this, playerDataDB, configManager, salvageLog);
        this.salvageGUI = new SalvageGUI(this, effectsUtil, statistics, configManager, salvageLog);
        this.talentTree = new TalentTree(this, playerDataDB, configManager);
        this.itemsDropTable = new ItemsDropTable(this, configManager, talentTree);
        this.playerData = new PlayerData(this, playerDataDB, bossBarUtil, configManager, effectsUtil, artefactItems);
        this.talentGUI = new TalentGUI(this, talentTree, playerData, effectsUtil, configManager);
        this.sellGUI = new SellGUI(this, effectsUtil, playerData, configManager, talentTree, statistics, sellLog);
        this.collectionsGUI = new CollectionsGUI(this, effectsUtil, configManager, playerData, sellGUI);
        this.collectionLog = new CollectionLog(this, playerDataDB, configManager, effectsUtil);
        this.artefactCollections = new ArtefactCollections(this, effectsUtil, collectionLog, configManager);
        this.itemCommands = new ItemCommands(this, customItems, customTools, customCharms, miscItems, artefactCollections, craftingMaterials, customAugments);
        this.collectionLogGUI = new CollectionLogGUI(this, effectsUtil, configManager, customItems, artefactCollections, collectionLog);
        this.rankGUI = new RankGUI(this, effectsUtil, playerData, statistics, configManager, collectionLog);
        this.playerDataCommands = new PlayerDataCommands(this, playerData, talentTree, rankGUI, effectsUtil, statistics);
        this.traitGUI = new TraitGUI(this, effectsUtil, playerData, configManager);
        this.itemEquipListener = new ItemEquipListener(this, customTools, playerData, talentTree, itemsDropTable, configManager);
        this.expManager = new EXPManager(this, statistics, playerData, talentTree, effectsUtil, configManager);
        this.lampStarGUI = new LampStarGUI(this, effectsUtil, expManager, configManager);
        this.itemInteractListener = new ItemInteractListener(this, collectionsGUI, miscItems, lampStarGUI);
        this.restorationStation = new RestorationStation(this, effectsUtil, expManager, playerData, statistics, artefactItems, configManager, artefactCollections);
        this.craftingStation = new CraftingStation(this, effectsUtil, playerData, statistics, customTools, configManager, customItems, craftingMaterials);
        this.blockCommands = new BlockCommands(this, restorationStation, craftingStation);
        this.blockBreakListener = new BlockBreakListener(this, playerData, customItems, customTools, customCharms, itemsDropTable, statistics, collectionLog, expManager, configManager, itemEquipListener, effectsUtil, craftingMaterials);
        this.playerJoinListener = new PlayerJoinListener(this, playerDataDB, playerData, talentTree, statistics, collectionLog, itemEquipListener, itemsDropTable, blockBreakListener, sellGUI, rankGUI);
        this.guiCommands = new GUICommands(this, sellGUI, salvageGUI, traitGUI, restorationStation, rankGUI, collectionLogGUI, talentGUI);

        registerPAPI();
        startUpdateTask();
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .verboseOutput(true)
                .silentLogs(false)
        );
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        restorationStation.cleanupAllCooldowns();
        craftingStation.cleanupAllCooldowns();
        if (playerData != null) {
            playerData.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save player data.");
        }
        if (talentTree != null) {
            talentTree.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save talent data.");
        }
        if (statistics != null) {
            statistics.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save statistics data.");
        }
        if (collectionLog != null) {
            collectionLog.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save statistics data.");
        }
        if (playerDataDB != null) {
            playerDataDB.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    playerData.updateAllData();
                    statistics.updateAllData();
                    talentTree.updateAllData();
                    collectionLog.updateAllData();
                    blockBreakListener.cleanupExpiredADPCooldowns();
                } catch (Exception e) {
                    getLogger().severe("[vArchaeology] An error occurred while updating data to database.");
                }
            }
        }.runTaskTimerAsynchronously(this, 12000L, 12000L);
    }

    public static Economy getEconomy() {
        return econ;
    }

    private void registerPAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderPlayerData = new PlaceholderPlayerData(this, playerData, statistics, configManager, rankGUI);
        } else {
            getLogger().severe("[vArchaeology] Failed to register PlaceholderAPI support.");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("[vArchaeology] Vault plugin not found!");
            return false;
        }
        getLogger().info("[vArchaeology] Vault was found, attempting to get economy registration...");

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("[vArchaeology] No economy provider was registered with Vault!");
            return false;
        }
        econ = rsp.getProvider();
        getLogger().info("[vArchaeology] Successfully hooked into the economy: " + econ.getName());
        return true;
    }
}
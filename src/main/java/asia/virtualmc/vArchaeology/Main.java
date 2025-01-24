package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.blocks.SalvageStation;
import asia.virtualmc.vArchaeology.commands.GUICommands;
import asia.virtualmc.vArchaeology.commands.PlayerDataCommands;
import asia.virtualmc.vArchaeology.commands.ItemCommands;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.guis.SalvageGUI;
import asia.virtualmc.vArchaeology.guis.SellGUI;
import asia.virtualmc.vArchaeology.guis.TalentGUI;
import asia.virtualmc.vArchaeology.guis.TraitGUI;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;
import asia.virtualmc.vArchaeology.listeners.BlockBreakListener;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.listeners.ItemEquipListener;
import asia.virtualmc.vArchaeology.listeners.MiscListener;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinListener;
import asia.virtualmc.vArchaeology.logs.LogManager;
import asia.virtualmc.vArchaeology.logs.SalvageLog;
import asia.virtualmc.vArchaeology.storage.*;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin {
    // storage
    private PlayerData playerData;
    private Statistics statistics;
    private PlayerDataDB playerDataDB;
    private TalentTree talentTree;
    private CollectionLog collectionLog;
    // items
    private ItemManager itemManager;
    private ItemCommands itemCommands;
    // utils
    private BossBarUtil bossBarUtil;
    private EffectsUtil effectsUtil;
    // items
    private RNGManager rngManager;
    // configs
    private ConfigManager configManager;
    // listeners
    private MiscListener miscListener;
    private BlockBreakListener blockBreakListener;
    private PlayerJoinListener playerJoinListener;
    private ItemEquipListener itemEquipListener;
    // blocks
    private SalvageStation salvageStation;
    // exp
    private EXPManager expManager;
    // guis
    private SellGUI sellGUI;
    private TalentGUI talentGUI;
    private SalvageGUI salvageGUI;
    private TraitGUI traitGUI;
    // logs
    private LogManager logManager;
    private SalvageLog salvageLog;
    // commands
    private PlayerDataCommands playerDataCommands;
    private GUICommands guiCommands;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        this.configManager = new ConfigManager(this);
        this.bossBarUtil = new BossBarUtil(this);
        this.effectsUtil = new EffectsUtil(this);
        this.itemManager = new ItemManager(this);
        this.itemCommands = new ItemCommands(this, itemManager);
        this.miscListener = new MiscListener(this);
        this.logManager = new LogManager(this);
        this.salvageLog = new SalvageLog(this, logManager);
        this.sellGUI = new SellGUI(this, effectsUtil);
        this.talentGUI = new TalentGUI(this, effectsUtil);
        this.playerDataDB = new PlayerDataDB(this, configManager);
        this.statistics = new Statistics(this, playerDataDB, configManager);
        this.salvageGUI = new SalvageGUI(this, effectsUtil, statistics, configManager, salvageLog);
        this.salvageStation = new SalvageStation(this, salvageGUI);
        this.talentTree = new TalentTree(this, playerDataDB, configManager);
        this.rngManager = new RNGManager(this, configManager, talentTree);
        this.playerData = new PlayerData(this, playerDataDB, bossBarUtil, configManager, effectsUtil);
        this.collectionLog = new CollectionLog(this, playerDataDB, configManager);
        this.playerDataCommands = new PlayerDataCommands(this, playerData, talentTree);
        this.traitGUI = new TraitGUI(this, effectsUtil, playerData, configManager);
        this.guiCommands = new GUICommands(this, sellGUI, salvageGUI, traitGUI);
        this.itemEquipListener = new ItemEquipListener(this, itemManager, playerData, talentTree, rngManager);
        this.playerJoinListener = new PlayerJoinListener(this, playerDataDB, playerData, talentTree, statistics, collectionLog, itemEquipListener, rngManager);
        this.expManager = new EXPManager(this, statistics, playerData, talentTree);
        this.blockBreakListener = new BlockBreakListener(this, playerData, itemManager, rngManager, statistics, expManager, configManager, itemEquipListener, effectsUtil);

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
        salvageStation.cleanupAllCooldowns();
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
}
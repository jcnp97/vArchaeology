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
import asia.virtualmc.vArchaeology.listeners.MiscListener;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinListener;
import asia.virtualmc.vArchaeology.logs.LogManager;
import asia.virtualmc.vArchaeology.logs.SalvageLogTransaction;
import asia.virtualmc.vArchaeology.storage.PlayerDataDB;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin {
    // storage
    private PlayerData playerData;
    // items
    private ItemManager itemManager;
    private ItemCommands itemCommands;
    private PlayerData PlayerData;
    private BossBarUtil bossBarUtil;
    private RNGManager rngManager;
    private ConfigManager configManager;
    private PlayerDataDB playerDataDB;
    private TalentTreeManager talentTreeManager;
    private StatsManager statsManager;
    private EffectsUtil effectsUtil;
    // listeners
    private MiscListener miscListener;
    private BlockBreakListener blockBreakListener;
    private PlayerJoinListener playerJoinListener;
    // blocks
    private SalvageStation salvageStation;
    // exp
    private EXPManager expManager;
    // guis
    private SellGUI sellGUI;
    private TalentGUI talentGUI;
    private SalvageGUI salvageGUI;
    private TraitGUI traitGUI;
    private GUICommands guiCommands;
    // logs
    private LogManager logManager;
    private SalvageLogTransaction salvageLogTransaction;
    // commands
    private PlayerDataCommands playerDataCommands;

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
        this.salvageLogTransaction = new SalvageLogTransaction(this, logManager);
        this.sellGUI = new SellGUI(this, effectsUtil);
        this.talentGUI = new TalentGUI(this, effectsUtil);
        this.rngManager = new RNGManager(this, configManager);
        this.playerDataDB = new PlayerDataDB(this, configManager);
        this.statsManager = new StatsManager(this, playerDataDB, configManager);
        this.salvageGUI = new SalvageGUI(this, effectsUtil, statsManager, configManager, salvageLogTransaction);
        this.salvageStation = new SalvageStation(this, salvageGUI);
        this.talentTreeManager = new TalentTreeManager(this, playerDataDB, configManager);
        this.playerData = new PlayerData(this, playerDataDB, bossBarUtil, configManager, effectsUtil);
        this.playerDataCommands = new PlayerDataCommands(this, playerData, talentTreeManager);
        this.traitGUI = new TraitGUI(this, effectsUtil, playerData, configManager);
        this.guiCommands = new GUICommands(this, sellGUI, salvageGUI, traitGUI);
        this.playerJoinListener = new PlayerJoinListener(this, playerDataDB, playerData, talentTreeManager, statsManager);
        this.expManager = new EXPManager(this, statsManager, playerData, talentTreeManager);
        this.blockBreakListener = new BlockBreakListener(this, playerData, itemManager, rngManager, statsManager, expManager);

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
        if (PlayerData != null) {
            PlayerData.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save player data.");
        }
        if (talentTreeManager != null) {
            talentTreeManager.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save talent data.");
        }
        if (statsManager != null) {
            statsManager.updateAllData();
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
                    PlayerData.updateAllData();
                    statsManager.updateAllData();
                    talentTreeManager.updateAllData();
                    blockBreakListener.cleanupExpiredADPCooldowns();
                } catch (Exception e) {
                    getLogger().severe("[vArchaeology] An error occurred while updating data to database.");
                }
            }
        }.runTaskTimerAsynchronously(this, 12000L, 12000L);
    }
}
package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;
import asia.virtualmc.vArchaeology.listeners.BlockBreakManager;
import asia.virtualmc.vArchaeology.listeners.CommandManager;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataDB;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin {
    private ItemManager itemManager;
    private PlayerDataManager playerDataManager;
    private PlayerJoinManager playerJoinManager;
    private CommandManager commandManager;
    private BlockBreakManager blockBreakManager;
    private BossBarUtil bossBarUtil;
    private RNGManager rngManager;
    private ConfigManager configManager;
    private PlayerDataDB playerDataDB;
    private TalentTreeManager talentTreeManager;
    private StatsManager statsManager;
    private EffectsUtil effectsUtil;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        this.configManager = new ConfigManager(this);
        this.bossBarUtil = new BossBarUtil(this);
        this.effectsUtil = new EffectsUtil(this);
        this.itemManager = new ItemManager(this);
        this.rngManager = new RNGManager(this, configManager);
        this.playerDataDB = new PlayerDataDB(this, configManager);
        this.statsManager = new StatsManager(this, playerDataDB, configManager);
        this.talentTreeManager = new TalentTreeManager(this, playerDataDB, configManager);
        this.playerDataManager = new PlayerDataManager(this, playerDataDB, bossBarUtil, configManager, effectsUtil);
        this.playerJoinManager = new PlayerJoinManager(this, playerDataDB, playerDataManager, talentTreeManager, statsManager);
        this.commandManager = new CommandManager(this, playerDataManager, itemManager, talentTreeManager, statsManager);
        this.blockBreakManager = new BlockBreakManager(this, playerDataManager, itemManager, rngManager, statsManager);

        getServer().getPluginManager().registerEvents(playerJoinManager, this);
        getServer().getPluginManager().registerEvents(blockBreakManager, this);
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
        if (playerDataManager != null) {
            playerDataManager.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save Player Data.");
        }
        if (talentTreeManager != null) {
            talentTreeManager.updateAllData();
        } else {
            getLogger().severe("[vArchaeology] Failed to save Talent Data.");
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
                ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Storing all data into database and cleaning up memory..");
                playerDataManager.updateAllData();
                statsManager.updateAllData();
                talentTreeManager.updateAllData();
                blockBreakManager.cleanupExpiredADPCooldowns();
            }
        }.runTaskTimerAsynchronously(this, 12000L, 12000L);
    }
}

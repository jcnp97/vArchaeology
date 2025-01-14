// Main.class
package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.listeners.BlockBreakManager;
import asia.virtualmc.vArchaeology.listeners.CommandManager;
import asia.virtualmc.vArchaeology.storage.DatabaseManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

import de.tr7zw.changeme.nbtapi.*;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private PlayerJoinManager playerJoinManager;
    private CommandManager commandManager;
    private BlockBreakManager blockBreakManager;
    private BossBarUtil bossBarUtil;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin");
            getPluginLoader().disablePlugin(this);
            return;
        }

        this.bossBarUtil = new BossBarUtil(this);
        this.databaseManager = new DatabaseManager(this);
        this.playerDataManager = new PlayerDataManager(this, databaseManager, bossBarUtil);
        this.playerJoinManager = new PlayerJoinManager(this, databaseManager, playerDataManager);
        this.commandManager = new CommandManager(this, playerDataManager);
        this.blockBreakManager = new BlockBreakManager(this, playerDataManager);

        getServer().getPluginManager().registerEvents(playerJoinManager, this);
        getServer().getPluginManager().registerEvents(blockBreakManager, this);
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
        }
        ConsoleMessageUtil.sendConsoleMessage("<#FFFF55>[vArchaeology] Closing database connections..");
        if (databaseManager != null) {
            databaseManager.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }
}

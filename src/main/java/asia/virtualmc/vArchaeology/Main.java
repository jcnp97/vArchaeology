package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.storage.DatabaseManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private PlayerJoinManager playerJoinManager;

    @Override
    public void onEnable() {
        this.databaseManager = new DatabaseManager(this);
        this.playerDataManager = new PlayerDataManager(this, databaseManager);
        this.playerJoinManager = new PlayerJoinManager(this, databaseManager, playerDataManager);

        getServer().getPluginManager().registerEvents(playerJoinManager, this);
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.updateAllData(); // Save all data before shutdown
        }
        ConsoleMessageUtil.sendConsoleMessage("<#FFFF55>[vArchaeology] Closing database connections..");
        if (databaseManager != null) {
            databaseManager.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }
}

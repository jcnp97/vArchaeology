package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.storage.DatabaseManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;

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
        getLogger().info("[vArchaeology] has been enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.updateAllData(); // Save all data before shutdown
        }
        getLogger().info("[vArchaeology] Closing database connections..");
        if (databaseManager != null) {
            databaseManager.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }
}

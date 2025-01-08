package asia.virtualmc.vArchaeology;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;
import asia.virtualmc.vArchaeology.storage.DatabaseManager;

public final class Main extends JavaPlugin {
    private DatabaseManager databaseManager;
    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);
        databaseManager.setupDatabase();
        getLogger().info("[vArchaeology] has been enabled!");

        getServer().getPluginManager().registerEvents(new PlayerJoinManager(this, databaseManager), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                databaseManager.updateAllPlayerData();
            }
        }.runTaskTimerAsynchronously(this, 0, 12000); // 12000 ticks = 10 minutes
    }

    @Override
    public void onDisable() {
        getLogger().info("[vArchaeology] Closing database connections..");
        if (databaseManager != null) {
            databaseManager.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }
}

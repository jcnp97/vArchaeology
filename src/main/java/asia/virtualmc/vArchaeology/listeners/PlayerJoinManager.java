package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinManager implements Listener {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final PlayerDataManager playerDataManager;

    public PlayerJoinManager(Main plugin, DatabaseManager databaseManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player exists in database
                if (databaseManager.getPlayerData(event.getPlayer().getUniqueId()).next()) {
                    // Player exists, load their data
                    playerDataManager.loadData(event.getPlayer().getUniqueId());
                } else {
                    // Player doesn't exist, create new data
                    databaseManager.savePlayerData(
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getName(),
                            0, // Default EXP
                            1, // Default Level
                            0.0, // Default Break Chance
                            1.0  // Default Gather Rate
                    );
                    playerDataManager.loadData(event.getPlayer().getUniqueId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Update player data in database before unloading
            PlayerData data = playerDataManager.getPlayerData(event.getPlayer().getUniqueId());
            if (data != null) {
                databaseManager.savePlayerData(
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getName(),
                        data.getExp(),
                        data.getLevel(),
                        data.getBreakChance(),
                        data.getGatherRate()
                );
                playerDataManager.unloadData(event.getPlayer().getUniqueId());
            }
        });
    }
}

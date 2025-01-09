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
                    databaseManager.createNewPlayerData(
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getName()
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
                        data.getArchExp(),
                        data.getArchLevel(),
                        data.getArchApt(),
                        data.getArchLuck(),
                        data.getArchADP(),
                        data.getArchXPMul(),
                        data.getArchBonusXP(),
                        data.getBlocksMined(),
                        data.getArtefactsFound(),
                        data.getArtefactsRestored(),
                        data.getTreasuresFound()
                );
                playerDataManager.unloadData(event.getPlayer().getUniqueId());
            }
        });
    }
}

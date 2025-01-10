package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.DatabaseManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

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
        UUID playerUUID = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player exists in the database
                if (databaseManager.getPlayerData(playerUUID).next()) {
                    // Load existing player data
                    playerDataManager.loadData(playerUUID);
                } else {
                    // Create new player data in the database
                    databaseManager.createNewPlayerData(playerUUID, playerName);
                    playerDataManager.loadData(playerUUID);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading player data for " + playerName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = playerDataManager.getPlayerData(playerUUID);
            if (data != null) {
                databaseManager.savePlayerData(
                        playerUUID,
                        data.getName(),
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
                playerDataManager.unloadData(playerUUID);
            }
        });
    }
}

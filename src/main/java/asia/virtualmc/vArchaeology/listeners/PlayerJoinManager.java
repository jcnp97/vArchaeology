package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerDataDB;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeDB;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerJoinManager implements Listener {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final PlayerDataManager playerDataManager;
    private final TalentTreeDB talentTreeDB;

    public PlayerJoinManager(Main plugin, PlayerDataDB playerDataDB, PlayerDataManager playerDataManager, TalentTreeDB talentTreeDB) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.playerDataManager = playerDataManager;
        this.talentTreeDB = talentTreeDB;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player exists in the database
                if (playerDataDB.getPlayerData(playerUUID).next()) {
                    // Load existing player data
                    playerDataManager.loadData(playerUUID);
                } else {
                    // Create new player data in the database
                    playerDataDB.createNewPlayerData(playerUUID, playerName);
                    talentTreeDB.createNewPlayerTalent(playerUUID);
                    // Load them into memory
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
            if (playerDataManager.getPlayerName(playerUUID) != null) {
                playerDataManager.updateAllData();
                playerDataManager.unloadData(playerUUID);
            }
        });
    }
}

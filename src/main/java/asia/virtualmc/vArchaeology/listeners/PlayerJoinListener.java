package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.PlayerDataDB;

import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final PlayerData playerData;
    private final TalentTreeManager talentTreeManager;
    private final StatsManager statsManager;

    public PlayerJoinListener(Main plugin, PlayerDataDB playerDataDB, PlayerData playerData, TalentTreeManager talentTreeManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.playerData = playerData;
        this.talentTreeManager = talentTreeManager;
        this.statsManager = statsManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player exists in the database
                if (playerDataDB.getPlayerData(playerUUID).next()) {
                    playerData.loadData(playerUUID);
                } else {
                    playerDataDB.createNewPlayerData(playerUUID, playerName);
                    playerData.loadData(playerUUID);
                    statsManager.loadData(playerUUID);
                }
                // Load other data since main data is loaded.
                talentTreeManager.loadData(playerUUID);
                statsManager.loadData(playerUUID);
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
            if (plugin.getServer().getPlayer(playerUUID) == null) {
                try {
                    playerData.unloadData(playerUUID);
                    talentTreeManager.unloadData(playerUUID);
                    statsManager.unloadData(playerUUID);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error unloading data for player: " + playerUUID);
                    e.printStackTrace();
                }
            }
        });
    }
}
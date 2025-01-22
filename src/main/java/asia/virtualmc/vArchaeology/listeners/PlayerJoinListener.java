package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.PlayerDataDB;

import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.storage.TalentTree;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final PlayerData playerData;
    private final TalentTree talentTree;
    private final Statistics statistics;

    public PlayerJoinListener(Main plugin, PlayerDataDB playerDataDB, PlayerData playerData, TalentTree talentTree, Statistics statistics) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.playerData = playerData;
        this.talentTree = talentTree;
        this.statistics = statistics;
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
                    statistics.loadData(playerUUID);
                }
                // Load other data since main data is loaded.
                talentTree.loadData(playerUUID);
                statistics.loadData(playerUUID);
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
                    talentTree.unloadData(playerUUID);
                    statistics.unloadData(playerUUID);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error unloading data for player: " + playerUUID);
                    e.printStackTrace();
                }
            }
        });
    }
}
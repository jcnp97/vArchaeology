package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.droptables.ItemsDropTable;
import asia.virtualmc.vArchaeology.guis.RankGUI;
import asia.virtualmc.vArchaeology.guis.SellGUI;
import asia.virtualmc.vArchaeology.storage.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Random;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final PlayerData playerData;
    private final TalentTree talentTree;
    private final Statistics statistics;
    private final CollectionLog collectionLog;
    private final ItemEquipListener itemEquipListener;
    private final ItemsDropTable itemsDropTable;
    private final BlockBreakListener blockBreakListener;
    private final SellGUI sellGUI;
    private final RankGUI rankGUI;

    public PlayerJoinListener(Main plugin,
                              PlayerDataDB playerDataDB,
                              PlayerData playerData,
                              TalentTree talentTree,
                              Statistics statistics,
                              CollectionLog collectionLog,
                              ItemEquipListener itemEquipListener,
                              ItemsDropTable itemsDropTable,
                              BlockBreakListener blockBreakListener,
                              SellGUI sellGUI,
                              RankGUI rankGUI
    ) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.playerData = playerData;
        this.talentTree = talentTree;
        this.statistics = statistics;
        this.collectionLog = collectionLog;
        this.itemEquipListener = itemEquipListener;
        this.itemsDropTable = itemsDropTable;
        this.blockBreakListener = blockBreakListener;
        this.sellGUI = sellGUI;
        this.rankGUI = rankGUI;

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
                    collectionLog.loadData(playerUUID);
                }
                // Load other data since main data is loaded.
                talentTree.loadData(playerUUID);
                statistics.loadData(playerUUID);
                collectionLog.loadData(playerUUID);
                sellGUI.loadPlayerSellData(playerUUID);
                rankGUI.loadData(playerUUID);
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
                    collectionLog.unloadData(playerUUID);
                    itemEquipListener.unloadToolData(playerUUID);
                    sellGUI.unloadSellData(playerUUID);
                    blockBreakListener.unloadTraitData(playerUUID);
                    itemsDropTable.unloadData(playerUUID);
                    rankGUI.unloadData(playerUUID);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error unloading data for player: " + playerUUID);
                    e.printStackTrace();
                }
            }
        });
    }
}
package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;
import asia.virtualmc.vArchaeology.configs.ConfigManager;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionLog {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Integer>> playerCollections;

    public CollectionLog(Main plugin, PlayerDataDB playerDataDB, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.configManager = configManager;
        this.playerCollections = new ConcurrentHashMap<>();
        createCollectionTables();
    }

    public void createCollectionTables() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archCollections (" +
                            "itemID INT NOT NULL AUTO_INCREMENT," +
                            "itemName VARCHAR(255) NOT NULL," +
                            "PRIMARY KEY (itemID)" +
                            ")"
            );
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archPlayerCollections (" +
                            "UUID VARCHAR(36) NOT NULL," +
                            "itemID INT NOT NULL," +
                            "amount INT DEFAULT 0," +
                            "PRIMARY KEY (UUID, itemID)," +
                            "FOREIGN KEY (itemID) REFERENCES archCollections(itemID)" +
                            "ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")"
            );
            List<String> collectionData = new ArrayList<>(configManager.loadCollections());
//            List<String> statList = Arrays.asList("commonGained", "uncommonGained", "rareGained",
//                    "uniqueGained", "specialGained", "mythicalGained", "exoticGained"
//            );

            String checkQuery = "SELECT COUNT(*) FROM archCollections WHERE itemName = ?";
            String insertQuery = "INSERT INTO archCollections (itemName) VALUES (?)";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (String item : collectionData) {
                    checkStmt.setString(1, item);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            insertStmt.setString(1, item);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully created collection table.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create collection table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadData(@NotNull UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT itemID, amount FROM archPlayerCollections WHERE UUID = ?"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            ConcurrentHashMap<Integer, Integer> playerCollectionMap = new ConcurrentHashMap<>();
            while (rs.next()) {
                int itemID = rs.getInt("itemID");
                int amount = rs.getInt("amount");
                playerCollectionMap.put(itemID, amount);
            }
            if (playerCollectionMap.isEmpty()) {
                createNewPlayerStats(uuid);
            } else {
                playerCollections.put(uuid, playerCollectionMap);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to load collection data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updatePlayerData(UUID uuid) {
        ConcurrentHashMap<Integer, Integer> playerData = playerCollections.get(uuid);
        if (playerData == null) {
            Bukkit.getLogger().warning("[vArchaeology] Attempted to update data for player " +
                    uuid + " but no data was found in memory.");
            return;
        }
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerCollections SET amount = ? WHERE UUID = ? AND itemID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                ps.setInt(1, entry.getValue());
                ps.setString(2, uuid.toString());
                ps.setInt(3, entry.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update collection data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updateAllData() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerCollections SET amount = ? WHERE UUID = ? AND itemID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<UUID, ConcurrentHashMap<Integer, Integer>> playerEntry : playerCollections.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                ConcurrentHashMap<Integer, Integer> talents = playerEntry.getValue();
                for (Map.Entry<Integer, Integer> talentEntry : talents.entrySet()) {
                    ps.setInt(1, talentEntry.getValue());
                    ps.setString(2, playerUUID.toString());
                    ps.setInt(3, talentEntry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update collection data: " + e.getMessage());
        }
    }

    public void unloadData(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            ConcurrentHashMap<Integer, Integer> playerData = playerCollections.get(uuid);
            if (playerData != null) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE archPlayerCollections SET amount = ? WHERE UUID = ? AND itemID = ?"
                );
                conn.setAutoCommit(false);
                for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                    ps.setInt(1, entry.getValue());
                    ps.setString(2, uuid.toString());
                    ps.setInt(3, entry.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                playerCollections.remove(uuid);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to unload collection data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void createNewPlayerStats(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement talentStmt = conn.prepareStatement("SELECT itemID FROM archCollections");
            ResultSet rs = talentStmt.executeQuery();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO archPlayerCollections (UUID, itemID, amount) VALUES (?, ?, 0)"
            );
            while (rs.next()) {
                int currentitemID = rs.getInt("itemID");
                ps.setString(1, uuid.toString());
                ps.setInt(2, currentitemID);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create new player collection: " + e.getMessage());
        }
    }

    public int getCollections(UUID playerUUID, int itemID) {
        return playerCollections.getOrDefault(playerUUID, new ConcurrentHashMap<>())
                .getOrDefault(itemID, 0);
    }

    public Map<UUID, Map<Integer, Integer>> getPlayerCollections() {
        return new ConcurrentHashMap<>(playerCollections);
    }

    public void incrementCollection(UUID playerUUID, int itemID) {
        playerCollections.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .merge(itemID, 1, Integer::sum);
        updatePlayerData(playerUUID);
    }

    public void addCollection(UUID playerUUID, int itemID, int value) {
        playerCollections.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .merge(itemID, value, Integer::sum);
        updatePlayerData(playerUUID);
    }
}
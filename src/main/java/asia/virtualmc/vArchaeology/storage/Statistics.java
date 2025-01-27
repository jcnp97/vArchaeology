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

public class Statistics {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Integer>> playerStatistics;

    public Statistics(Main plugin, PlayerDataDB playerDataDB, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.configManager = configManager;
        this.playerStatistics = new ConcurrentHashMap<>();
        createStatisticsTables();
    }

    public void createStatisticsTables() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archStatistics (" +
                            "statsID INT NOT NULL AUTO_INCREMENT," +
                            "statsName VARCHAR(255) NOT NULL," +
                            "PRIMARY KEY (statsID)" +
                            ")"
            );
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archPlayerStatistics (" +
                            "UUID VARCHAR(36) NOT NULL," +
                            "statsID INT NOT NULL," +
                            "amount INT DEFAULT 0," +
                            "PRIMARY KEY (UUID, statsID)," +
                            "FOREIGN KEY (statsID) REFERENCES archStatistics(statsID)" +
                            "ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")"
            );
            // 1: Rank
            // 2 - 8: Components
            List<String> statList = Arrays.asList("rankAchieved", "commonComponents", "uncommonComponents",
                    "rareComponents", "uniqueComponents", "specialComponents", "mythicalComponents",
                    "exoticComponents", "blocksMined", "artefactsFound", "artefactsRestored",
                    "treasuresFound", "moneyEarned"
            );
            String checkQuery = "SELECT COUNT(*) FROM archStatistics WHERE statsName = ?";
            String insertQuery = "INSERT INTO archStatistics (statsName) VALUES (?)";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (String stat : statList) {
                    checkStmt.setString(1, stat);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            insertStmt.setString(1, stat);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully created statistics table.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create statistics table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadData(@NotNull UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT statsID, amount FROM archPlayerStatistics WHERE UUID = ?"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            ConcurrentHashMap<Integer, Integer> playerStatisticsMap = new ConcurrentHashMap<>();
            while (rs.next()) {
                int statsID = rs.getInt("statsID");
                int amount = rs.getInt("amount");
                playerStatisticsMap.put(statsID, amount);
            }
            if (playerStatisticsMap.isEmpty()) {
                createNewPlayerStats(uuid);
            } else {
                playerStatistics.put(uuid, playerStatisticsMap);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to load statistics data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updatePlayerData(UUID uuid) {
        ConcurrentHashMap<Integer, Integer> playerData = playerStatistics.get(uuid);
        if (playerData == null) {
            Bukkit.getLogger().warning("[vArchaeology] Attempted to update data for player " +
                    uuid + " but no data was found in memory.");
            return;
        }
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerStatistics SET amount = ? WHERE UUID = ? AND statsID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                ps.setInt(1, entry.getValue());        // amount
                ps.setString(2, uuid.toString());      // UUID
                ps.setInt(3, entry.getKey());         // statsID
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update statistics data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updateAllData() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerStatistics SET amount = ? WHERE UUID = ? AND statsID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<UUID, ConcurrentHashMap<Integer, Integer>> playerEntry : playerStatistics.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                ConcurrentHashMap<Integer, Integer> talents = playerEntry.getValue();
                for (Map.Entry<Integer, Integer> talentEntry : talents.entrySet()) {
                    ps.setInt(1, talentEntry.getValue());    // amount
                    ps.setString(2, playerUUID.toString());  // UUID
                    ps.setInt(3, talentEntry.getKey());      // statsID
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update statistics data: " + e.getMessage());
        }
    }

    public void unloadData(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            ConcurrentHashMap<Integer, Integer> playerData = playerStatistics.get(uuid);
            if (playerData != null) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE archPlayerStatistics SET amount = ? WHERE UUID = ? AND statsID = ?"
                );
                conn.setAutoCommit(false);
                for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                    ps.setInt(1, entry.getValue());        // amount
                    ps.setString(2, uuid.toString());      // UUID
                    ps.setInt(3, entry.getKey());         // statsID
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                playerStatistics.remove(uuid);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to unload statistics data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void createNewPlayerStats(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement talentStmt = conn.prepareStatement("SELECT statsID FROM archStatistics");
            ResultSet rs = talentStmt.executeQuery();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO archPlayerStatistics (UUID, statsID, amount) VALUES (?, ?, 0)"
            );
            while (rs.next()) {
                int currentstatsID = rs.getInt("statsID");
                ps.setString(1, uuid.toString());
                ps.setInt(2, currentstatsID);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create new player statistics: " + e.getMessage());
        }
    }

    public int getStatistics(UUID playerUUID, int statsID) {
        return playerStatistics.getOrDefault(playerUUID, new ConcurrentHashMap<>())
                .getOrDefault(statsID, 0);
    }

    public Map<UUID, Map<Integer, Integer>> getPlayerStatistics() {
        return new ConcurrentHashMap<>(playerStatistics);
    }

    public void incrementStatistics(UUID playerUUID, int statsID) {
        playerStatistics.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .merge(statsID, 1, Integer::sum);
        //updatePlayerData(playerUUID);
    }

    public void addStatistics(UUID playerUUID, int statsID, int value) {
        playerStatistics.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .merge(statsID, value, Integer::sum);
        //updatePlayerData(playerUUID);
    }

    public ArrayList<Integer> getComponents(UUID playerUUID) {
        ArrayList<Integer> componentsOwned = new ArrayList<>();
        for (int i = 2; i < 9; i++) {
            componentsOwned.add(playerStatistics.getOrDefault(playerUUID, new ConcurrentHashMap<>()).getOrDefault(i,0));
        }
        return componentsOwned;
    }

    public void subtractComponents(UUID playerUUID, List<Integer> componentsRequired) {
        ConcurrentHashMap<Integer, Integer> statsMap = playerStatistics.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        statsMap.merge(2, -componentsRequired.get(0), Integer::sum);
        statsMap.merge(3, -componentsRequired.get(1), Integer::sum);
        statsMap.merge(4, -componentsRequired.get(2), Integer::sum);
        statsMap.merge(5, -componentsRequired.get(3), Integer::sum);
        statsMap.merge(6, -componentsRequired.get(4), Integer::sum);
        statsMap.merge(7, -componentsRequired.get(5), Integer::sum);
        statsMap.merge(8, -componentsRequired.get(6), Integer::sum);
        updatePlayerData(playerUUID);
    }
}
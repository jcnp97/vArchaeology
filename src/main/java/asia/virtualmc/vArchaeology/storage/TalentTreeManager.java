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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.List;

public class TalentTreeManager {
    private final Main plugin;
    private final PlayerDataDB playerDataDB;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Integer>> playerTalents;

    public TalentTreeManager(Main plugin, PlayerDataDB playerDataDB, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.configManager = configManager;
        this.playerTalents = new ConcurrentHashMap<>();
        createTalentTables();
    }

    public void createTalentTables() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            // Create talentTree table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archTalentTree (" +
                            "talentID INT NOT NULL AUTO_INCREMENT," +
                            "talentName VARCHAR(255) NOT NULL," +
                            "PRIMARY KEY (talentID)" +
                            ")"
            );
            // Create playerTalent table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archPlayerTalent (" +
                            "UUID VARCHAR(36) NOT NULL," +
                            "talentID INT NOT NULL," +
                            "talentLevel INT DEFAULT 0," +
                            "PRIMARY KEY (UUID, talentID)," +
                            "FOREIGN KEY (talentID) REFERENCES archTalentTree(talentID)" +
                            "ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")"
            );
            // Add all Talent into Talent Tree table
            List<String> talentData = Arrays.asList("Sagacity", "Prosperity", "Insightful Judgement",
                    "Extraction", "Adept Restoration", "Archaeological Prowess", "Divine Opulence",
                    "Rapid Discovery", "Prudence", "Blessed Treatment", "Stroke of Luck"
            );
            String checkQuery = "SELECT COUNT(*) FROM archTalentTree WHERE talentName = ?";
            String insertQuery = "INSERT INTO archTalentTree (talentName) VALUES (?)";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (String talent : talentData) {
                    checkStmt.setString(1, talent);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            insertStmt.setString(1, talent);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully created talent data table.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create talent tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadData(@NotNull UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT talentID, talentLevel FROM archPlayerTalent WHERE UUID = ?"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            ConcurrentHashMap<Integer, Integer> playerTalentMap = new ConcurrentHashMap<>();
            while (rs.next()) {
                int talentID = rs.getInt("talentID");
                int talentLevel = rs.getInt("talentLevel");
                playerTalentMap.put(talentID, talentLevel);
            }
            if (playerTalentMap.isEmpty()) {
                createNewPlayerTalent(uuid);
            } else {
                playerTalents.put(uuid, playerTalentMap);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to load talent data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updatePlayerData(UUID uuid) {
        ConcurrentHashMap<Integer, Integer> playerData = playerTalents.get(uuid);
        if (playerData == null) {
            Bukkit.getLogger().warning("[vArchaeology] Attempted to update data for player " +
                    uuid + " but no data was found in memory.");
            return;
        }
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerTalent SET talentLevel = ? WHERE UUID = ? AND talentID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                ps.setInt(1, entry.getValue());        // talentLevel
                ps.setString(2, uuid.toString());      // UUID
                ps.setInt(3, entry.getKey());         // talentID
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update talent data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public void updateAllData() {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerTalent SET talentLevel = ? WHERE UUID = ? AND talentID = ?"
            );
            conn.setAutoCommit(false);
            for (Map.Entry<UUID, ConcurrentHashMap<Integer, Integer>> playerEntry : playerTalents.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                ConcurrentHashMap<Integer, Integer> talents = playerEntry.getValue();
                for (Map.Entry<Integer, Integer> talentEntry : talents.entrySet()) {
                    ps.setInt(1, talentEntry.getValue());    // talentLevel
                    ps.setString(2, playerUUID.toString());  // UUID
                    ps.setInt(3, talentEntry.getKey());      // talentID
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update talent data: " + e.getMessage());
        }
    }

    public void updateTalentLevel(UUID playerUUID, int talentID, int newLevel) {
        playerTalents.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .put(talentID, newLevel);
        updatePlayerData(playerUUID);
    }

    public void unloadData(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            ConcurrentHashMap<Integer, Integer> playerData = playerTalents.get(uuid);
            if (playerData != null) {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE archPlayerTalent SET talentLevel = ? WHERE UUID = ? AND talentID = ?"
                );
                conn.setAutoCommit(false);
                for (Map.Entry<Integer, Integer> entry : playerData.entrySet()) {
                    ps.setInt(1, entry.getValue());        // talentLevel
                    ps.setString(2, uuid.toString());      // UUID
                    ps.setInt(3, entry.getKey());         // talentID
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                playerTalents.remove(uuid);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to unload talent data for player " +
                    uuid + ": " + e.getMessage());
        }
    }

    public int getTalentLevel(UUID playerUUID, int talentID) {
        return playerTalents.getOrDefault(playerUUID, new ConcurrentHashMap<>())
                .getOrDefault(talentID, 0);
    }

    public void createNewPlayerTalent(UUID uuid) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement talentStmt = conn.prepareStatement("SELECT talentID FROM archTalentTree");
            ResultSet rs = talentStmt.executeQuery();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO archPlayerTalent (UUID, talentID, talentLevel) VALUES (?, ?, 0)"
            );
            while (rs.next()) {
                int currentTalentID = rs.getInt("talentID");
                ps.setString(1, uuid.toString());
                ps.setInt(2, currentTalentID);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create new player talent: " + e.getMessage());
        }
    }

    // beware, this returns a hashmap that can be
    public ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Integer>> getPlayerTalents() {
        return playerTalents;
    }
}
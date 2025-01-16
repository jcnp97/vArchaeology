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
import java.util.UUID;
import java.util.List;

public class TalentTreeDB {
    private Main plugin;
    private final PlayerDataDB playerDataDB;
    private final ConfigManager configManager;

    public TalentTreeDB(Main plugin, PlayerDataDB playerDataDB, ConfigManager configManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.configManager = configManager;
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
            ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully created TalentData tables.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create talent tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void savePlayerTalent(UUID uuid, int talentID, int newLevel) {
        try (Connection conn = playerDataDB.getDataSource().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerTalent SET talentLevel = ? WHERE UUID = ? AND talentID = ?"
            );
            ps.setInt(1, newLevel);
            ps.setString(2, uuid.toString());
            ps.setInt(3, talentID);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to update player talent: " + e.getMessage());
        }
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
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[vArchaeology] Failed to create new player talent: " + e.getMessage());
        }
    }
}
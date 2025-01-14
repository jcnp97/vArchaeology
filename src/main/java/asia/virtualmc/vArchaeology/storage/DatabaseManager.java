package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.UUID;

public class DatabaseManager {
    private Main plugin;
    private HikariDataSource dataSource;
    private String host;
    private String database;
    private String username;
    private String password;
    private int port;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    public void setupDatabase() {
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Create and load database.yml
        File dbConfigFile = new File(plugin.getDataFolder(), "database.yml");
        if (!dbConfigFile.exists()) {
            try {
                plugin.saveResource("database.yml", false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        FileConfiguration dbConfig = YamlConfiguration.loadConfiguration(dbConfigFile);
        try {
            host = dbConfig.getString("mysql.host", "localhost");
            port = dbConfig.getInt("mysql.port", 3306);
            database = dbConfig.getString("mysql.database", "minecraft");
            username = dbConfig.getString("mysql.username", "root");
            password = dbConfig.getString("mysql.password");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[vArchaeology] Error while connecting to the database.");
        }

        // Configure HikariCP using default values
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    public void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            ConsoleMessageUtil.sendConsoleMessage("<#00FFA2>[vArchaeology] Successfully create tables on your database.");
            // Create playerStats table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archPlayerStats (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "playerName VARCHAR(16) NOT NULL," +
                            "archEXP DECIMAL(13,2) DEFAULT 0.00," +
                            "archLevel TINYINT DEFAULT 1," +
                            "archApt INT DEFAULT 0," +
                            "archLuck TINYINT DEFAULT 0" +
                            ")"
            );
            // Create internalStats table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archInternalStats (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "archADP DECIMAL(5,2) DEFAULT 0.00," +
                            "archXPMul DECIMAL(4,2) DEFAULT 1.00," +
                            "archBonusXP INT DEFAULT 0," +
                            "FOREIGN KEY (playerUUID) REFERENCES archPlayerStats(playerUUID)" +
                            ")"
            );
            // Create achievementStats table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archAchievements (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "blocksMined INT DEFAULT 0," +
                            "artefactsFound INT DEFAULT 0," +
                            "artefactsRestored INT DEFAULT 0," +
                            "treasuresFound INT DEFAULT 0," +
                            "FOREIGN KEY (playerUUID) REFERENCES archPlayerStats(playerUUID)" +
                            ")"
            );
        } catch (SQLException e) {
            //e.printStackTrace();
            Bukkit.getLogger().severe("[vArchaeology] Failed to create tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        } else {
            throw new SQLException("DataSource is not initialized.");
        }
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void savePlayerData(
            UUID uuid, String name, double archExp, int archLevel, int archApt, int archLuck, double archADP,
            double archXPMul, int archBonusXP, int blocksMined, int artFound, int artRestored, int treasuresFound
    ) {
        try (Connection conn = dataSource.getConnection()) {
            // Update playerStats database from latest HashMap data
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerStats SET " +
                            "playerName = ?, " +
                            "archEXP = ?, " +
                            "archLevel = ?, " +
                            "archApt = ?, " +
                            "archLuck = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setString(1, name);
            ps.setDouble(2, archExp);
            ps.setInt(3, archLevel);
            ps.setInt(4, archApt);
            ps.setInt(5, archLuck);
            ps.setString(6, uuid.toString());
            ps.executeUpdate();

            // Update internalStats database from latest HashMap data
            ps = conn.prepareStatement(
                    "UPDATE archInternalStats SET " +
                            "archADP = ?, " +
                            "archXPMul = ?, " +
                            "archBonusXP = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setDouble(1, archADP);
            ps.setDouble(2, archXPMul);
            ps.setInt(3, archBonusXP);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();

            // Update archAchievements database from latest HashMap data
            ps = conn.prepareStatement(
                    "UPDATE archAchievements SET " +
                            "blocksMined = ?, " +
                            "artefactsFound = ?, " +
                            "artefactsRestored = ?, " +
                            "treasuresFound = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setInt(1, blocksMined);
            ps.setInt(2, artFound);
            ps.setInt(3, artRestored);
            ps.setInt(4, treasuresFound);
            ps.setString(5, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createNewPlayerData(UUID uuid, String name) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO archPlayerStats (" +
                                "playerUUID, " +
                                "playerName, " +
                                "archEXP, " +
                                "archLevel, " +
                                "archApt, " +
                                "archLuck" +
                                ") VALUES (?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, 0);
                ps.setInt(4, 1);
                ps.setInt(5, 0);
                ps.setInt(6, 0);
                ps.executeUpdate();

                ps = conn.prepareStatement(
                        "INSERT INTO archInternalStats (" +
                                "playerUUID, " +
                                "archADP, " +
                                "archXPMul, " +
                                "archBonusXP" +
                                ") VALUES (?, ?, ?, ?)"
                );
                ps.setString(1, uuid.toString());
                ps.setDouble(2, 0.0);
                ps.setDouble(3, 1.0);
                ps.setInt(4, 0);
                ps.executeUpdate();

                ps = conn.prepareStatement(
                        "INSERT INTO archAchievements (" +
                                "playerUUID, " +
                                "blocksMined, " +
                                "artefactsFound, " +
                                "artefactsRestored, " +
                                "treasuresFound" +
                                ") VALUES (?, ?, ?, ?, ?)"
                );
                ps.setString(1, uuid.toString());
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setInt(5, 0);
                ps.executeUpdate();

                // If all inserts succeed, commit the transaction
                conn.commit();
                ConsoleMessageUtil.sendConsoleMessage("<#00FFA2>[vArchaeology] Successfully created new player data for " + name);

            } catch (SQLException e) {
                // If any error occurs during data creation, roll back the transaction
                if (conn != null) {
                    conn.rollback();
                }
                plugin.getLogger().severe("[vArchaeology] Failed to create player data for " + name + ". Error: " + e.getMessage());
                throw e; // Re-throw to be caught by outer catch block
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("[vArchaeology] Database error while creating player data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    // Reset auto-commit to true before closing connection.
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    plugin.getLogger().severe("[vArchaeology] Error closing database connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public ResultSet getPlayerData(UUID uuid) {
        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.*, " +
                            "i.archADP, i.archXPMul, i.archBonusXP, " +
                            "a.blocksMined, a.artefactsFound, a.artefactsRestored, a.treasuresFound " +
                            "FROM archPlayerStats p " +
                            "LEFT JOIN archInternalStats i ON p.playerUUID = i.playerUUID " +
                            "LEFT JOIN archAchievements a ON p.playerUUID = a.playerUUID " +
                            "WHERE p.playerUUID = ?"
            );
            ps.setString(1, uuid.toString());
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

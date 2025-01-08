package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.Statement;
import java.util.UUID;
//import java.util.HashMap;

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

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
//        config.addDataSourceProperty("cachePrepStmts", "true");
//        config.addDataSourceProperty("prepStmtCacheSize", "250");
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        createTables();
//        try (Connection connection = getConnection()) {
//            if (connection != null) {
//                Bukkit.getLogger().info("[vArchaeology] Successfully connected to the database.");
//
//            }
//        } catch (SQLException e) {
//            Bukkit.getLogger().severe("[vArchaeology] Database connection failed: " + e.getMessage());
//        }
    }

    public void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            Bukkit.getLogger().info("[vArchaeology] Successfully connected to the database.");
            // Create playerStats table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archPlayerStats (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "playerName VARCHAR(16) NOT NULL," +
                            "playerArchEXP DECIMAL(12,2) DEFAULT 0.00," +
                            "playerArchLevel TINYINT DEFAULT 1" +
                            "playerArchApt INT DEFAULT 0" +
                            "playerArchLuck TINYINT DEFAULT 0" +
                            ")"
            );
            // Create internalStats table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archInternalStats (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "archADP DECIMAL(5,2) DEFAULT 0.00," +
                            "archXPMul DECIMAL(4,2) DEFAULT 0.00," +
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
            UUID uuid, String name, int archExp, int archLevel, int archApt, int archLuck, double archADP,
            double archXPMul, int archBonusXP, int blocksMined, int artFound, int artRestored, int treasuresFound
    ) {
        try (Connection conn = dataSource.getConnection()) {
            // Update playerStats database from latest HashMap data
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE archPlayerStats SET " +
                            "playerName = ?, " +
                            "playerArchEXP = ?, " +
                            "playerArchLevel = ? " +
                            "playerArchApt = ? " +
                            "playerArchLuck = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setString(1, name);
            ps.setInt(2, archExp);
            ps.setInt(3, archLevel);
            ps.setInt(4, archApt);
            ps.setInt(5, archLuck);
            ps.setString(6, uuid.toString());
            ps.executeUpdate();

            // Update internalStats database from latest HashMap data
            ps = conn.prepareStatement(
                    "UPDATE archInternalStats SET " +
                            "archADP = ?, " +
                            "archXPMul = ? " +
                            "archBonusXP = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setDouble(1, archADP);
            ps.setDouble(2, archXPMul);
            ps.setDouble(3, archBonusXP);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();

            // Update archAchievements database from latest HashMap data
            ps = conn.prepareStatement(
                    "UPDATE archAchievements SET " +
                            "blocksMined = ?, " +
                            "artefactsFound = ? " +
                            "artefactsRestored = ? " +
                            "treasuresFound = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setDouble(1, blocksMined);
            ps.setDouble(2, artFound);
            ps.setDouble(3, artRestored);
            ps.setDouble(4, treasuresFound);
            ps.setString(5, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

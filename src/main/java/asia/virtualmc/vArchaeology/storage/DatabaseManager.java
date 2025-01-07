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
                    "CREATE TABLE IF NOT EXISTS playerStats (" +
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
                    "CREATE TABLE IF NOT EXISTS internalStats (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "breakChance DOUBLE DEFAULT 0.0," +
                            "gatherRate DOUBLE DEFAULT 1.0," +
                            "FOREIGN KEY (playerUUID) REFERENCES playerStats(playerUUID)" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }





        String archPlayerStatsTable = "CREATE TABLE IF NOT EXISTS archPlayerStats (" +
                "playerUUID CHAR(36) PRIMARY KEY, " +
                "playerName VARCHAR(16), " +
                "playerArchEXP DECIMAL(12,2) DEFAULT 0.00, " +
                "playerArchLevel TINYINT DEFAULT 0, " +
                "playerArchApt INT DEFAULT 0, " +
                "playerArchLuck TINYINT DEFAULT 0);"
                ;

        String archInternalStatsTable = "CREATE TABLE IF NOT EXISTS archInternalStats (" +
                "playerUUID CHAR(36) PRIMARY KEY, " +
                "archBreakChance TINYINT DEFAULT 0, " +
                "archGatherRate DECIMAL(5,2) DEFAULT 0.00, " +
                "archADP DECIMAL(5,2) DEFAULT 0.00, " +
                "archADB DECIMAL(4,2) DEFAULT 0.00, " +
                "archXPMul DECIMAL(4,2) DEFAULT 0.00, " +
                "archBonusXP INT DEFAULT 0);"
                ;

        String archAchievementsTable = "CREATE TABLE IF NOT EXISTS archAchievements (" +
                "playerUUID CHAR(36) PRIMARY KEY, " +
                "blocksMined INT DEFAULT 0, " +
                "artefactsFound INT DEFAULT 0, " +
                "artefactsRestored INT DEFAULT 0, " +
                "treasuresFound INT DEFAULT 0);"
                ;

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(archPlayerStatsTable);
            statement.executeUpdate(archInternalStatsTable);
            statement.executeUpdate(archAchievementsTable);
        } catch (SQLException e) {
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

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

//    public void updateAllPlayerData() {
//        for (UUID uuid : playerDataMap.keySet()) {
//            updatePlayerData(uuid);
//        }
//    }

    public void updatePlayerData(UUID uuid) {
        PlayerDataManager data = playerDataMap.get(uuid);
        if (data == null) return;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "REPLACE INTO archPlayerStats (" +
                             "playerUUID, " +
                             "playerName, " +
                             "playerArchEXP, " +
                             "playerArchLevel, " +
                             "playerArchApt, " +
                             "playerArchLuck)" +
                             "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, data.getPlayerName());
            statement.setBigDecimal(3, data.getPlayerArchEXP());
            statement.setInt(4, data.getPlayerArchLevel());
            statement.setInt(5, data.getPlayerArchApt());
            statement.setInt(6, data.getPlayerArchLuck());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update player data for UUID: " + uuid + ", Error: " + e.getMessage());
        }
    }

    public HashMap<UUID, PlayerDataManager> getPlayerDataMap() {
        return playerDataMap;
    }
}

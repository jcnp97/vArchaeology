package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;
import asia.virtualmc.vArchaeology.configs.ConfigManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.UUID;

public class PlayerDataDB {
    private final Main plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public PlayerDataDB(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupDatabase();
    }

    public void setupDatabase() {
        // Configure HikariCP using default values
        try {
            HikariConfig config = createHikariConfig();
            dataSource = new HikariDataSource(config);

            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully connected to the MySQL database.");
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("[vArchaeology] Failed to connect to database: " + e.getMessage());
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[vArchaeology] Error during database connection: " + e.getMessage());
        }
        createTables();
    }

    private HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + configManager.host + ":" + configManager.port + "/" + configManager.dbname);
        config.setUsername(configManager.username);
        config.setPassword(configManager.password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return config;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            ConsoleMessageUtil.sendConsoleMessage(configManager.pluginPrefix + "<#7CFEA7>Successfully created player data tables.");
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
                            "traitPoints INT DEFAULT 1," +
                            "talentPoints INT DEFAULT 0," +
                            "FOREIGN KEY (playerUUID) REFERENCES archPlayerStats(playerUUID)" +
                            ")"
            );
            // Create innateTrait table
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS archTraits (" +
                            "playerUUID VARCHAR(36) PRIMARY KEY," +
                            "wisdomTrait INT DEFAULT 0," +
                            "charismaTrait INT DEFAULT 0," +
                            "karmaTrait INT DEFAULT 0," +
                            "dexterityTrait INT DEFAULT 0," +
                            "FOREIGN KEY (playerUUID) REFERENCES archPlayerStats(playerUUID)" +
                            ")"
            );
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

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void savePlayerData(
            @NotNull UUID uuid, String name, double archExp, int archLevel, int archApt, int archLuck, double archADP,
            double archXPMul, int archBonusXP, int traitPoints, int talentPoints, int wisdomTrait, int charismaTrait,
            int karmaTrait, int dexterityTrait
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
                            "archBonusXP = ?, " +
                            "traitPoints = ?, " +
                            "talentPoints = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setDouble(1, archADP);
            ps.setDouble(2, archXPMul);
            ps.setInt(3, archBonusXP);
            ps.setInt(4, traitPoints);
            ps.setInt(5, talentPoints);
            ps.setString(6, uuid.toString());
            ps.executeUpdate();

            // Update archTraits database from latest HashMap data
            ps = conn.prepareStatement(
                    "UPDATE archTraits SET " +
                            "wisdomTrait = ?, " +
                            "charismaTrait = ?, " +
                            "karmaTrait = ?, " +
                            "dexterityTrait = ? " +
                            "WHERE playerUUID = ?"
            );
            ps.setInt(1, wisdomTrait);
            ps.setInt(2, charismaTrait);
            ps.setInt(3, karmaTrait);
            ps.setInt(4, dexterityTrait);
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
                                "archLuck " +
                                ") VALUES (?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, 0);
                ps.setInt(4, 1);
                ps.setInt(5, 0);
                ps.setInt(6, 0);
                ps.executeUpdate();
                // archInternalStats
                ps = conn.prepareStatement(
                        "INSERT INTO archInternalStats (" +
                                "playerUUID, " +
                                "archADP, " +
                                "archXPMul, " +
                                "archBonusXP, " +
                                "traitPoints, " +
                                "talentPoints " +
                                ") VALUES (?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, uuid.toString());
                ps.setDouble(2, 0.0);
                ps.setDouble(3, 1.0);
                ps.setInt(4, 0);
                ps.setInt(5, 1);
                ps.setInt(6, 0);
                ps.executeUpdate();
                // archTraits
                ps = conn.prepareStatement(
                        "INSERT INTO archTraits (" +
                                "playerUUID, " +
                                "wisdomTrait, " +
                                "charismaTrait, " +
                                "karmaTrait, " +
                                "dexterityTrait " +
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
                            "i.archADP, i.archXPMul, i.archBonusXP, i.traitPoints, i.talentPoints," +
                            "t.wisdomTrait, t.charismaTrait, t.karmaTrait, t.dexterityTrait " +
                            "FROM archPlayerStats p " +
                            "LEFT JOIN archInternalStats i ON p.playerUUID = i.playerUUID " +
                            "LEFT JOIN archTraits t ON p.playerUUID = t.playerUUID " +
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

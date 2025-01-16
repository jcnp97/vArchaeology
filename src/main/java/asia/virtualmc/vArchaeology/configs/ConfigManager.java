package asia.virtualmc.vArchaeology.configs;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ConfigManager {
    private Main plugin;
    // database.yml
    public String host;
    public String dbname;
    public String username;
    public String password;
    public int port;
    // others
    public String pluginPrefix = "<#0040FF>[vArch<#00FBFF>aeology] ";

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        readConfigs();
    }

    public void readConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        // Read and load database.yml
        readDatabase();
    }

    public void readDatabase() {
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
            dbname = dbConfig.getString("mysql.database", "minecraft");
            username = dbConfig.getString("mysql.username", "root");
            password = dbConfig.getString("mysql.password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

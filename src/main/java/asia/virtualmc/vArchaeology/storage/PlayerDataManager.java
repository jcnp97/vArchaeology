package asia.virtualmc.vArchaeology.storage;
import asia.virtualmc.vArchaeology.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class PlayerDataManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataMap;
    private final Map<Integer, Integer> experienceTable = new HashMap<>();
    private BukkitTask updateTask;

    public PlayerDataManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerDataMap = new HashMap<>();
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateAllData, 12000L, 12000L);
    }

    public void updateAllData() {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            PlayerData data = entry.getValue();
            databaseManager.savePlayerData(
                    entry.getKey(),
                    data.getName(),
                    data.getArchExp(),
                    data.getArchLevel(),
                    data.getArchApt(),
                    data.getArchLuck(),
                    data.getArchADP(),
                    data.getArchXPMul(),
                    data.getArchBonusXP(),
                    data.getBlocksMined(),
                    data.getArtefactsFound(),
                    data.getArtefactsRestored(),
                    data.getTreasuresFound()
            );
        }
    }

    public void loadData(UUID uuid) {
        try (ResultSet rs = databaseManager.getPlayerData(uuid)) {
            if (rs.next()) {
                PlayerData data = new PlayerData(
                        rs.getString("playerName"),
                        rs.getInt("archEXP"),
                        rs.getInt("archLevel"),
                        rs.getInt("archApt"),
                        rs.getInt("archLuck"),
                        rs.getDouble("archADP"),
                        rs.getDouble("archXPMul"),
                        rs.getInt("archBonusXP"),
                        rs.getInt("blocksMined"),
                        rs.getInt("artefactsFound"),
                        rs.getInt("artefactsRestored"),
                        rs.getInt("treasuresFound")
                );
                playerDataMap.put(uuid, data);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unloadData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void updateExp(UUID uuid, int exp, String param) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            switch (param.toLowerCase()) {
                case "add":
                    data.addArchEXP(exp);
                    break;
                case "sub":
                    data.subtractArchEXP(exp);
                    break;
                default:
                    data.setArchEXP(exp);
                    break;
            }
        }
    }

    public void updateLevel(UUID uuid, int level, String param) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            switch (param.toLowerCase()) {
                case "add":
                    data.addArchLevel(level);
                    break;
                case "sub":
                    data.subtractArchLevel(level);
                    break;
                default:
                    data.setArchLevel(level);
                    break;
            }
        }
    }

    public void loadExperienceTable() {
        File expTableFile = new File(plugin.getDataFolder(), "experience-table.yml");
        if (!expTableFile.exists()) {
            try {
                plugin.saveResource("experience-table.yml", false);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Experience table not found. Disabling the plugin..");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(expTableFile);
        int previousExp = -1;

        for (String key : config.getKeys(false)) {
            int level;
            int exp;

            try {
                level = Integer.parseInt(key);
                String expString = config.getString(key).replace(",", "");
                exp = Integer.parseInt(expString);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().severe("Invalid formatting on experience table. Disabling the plugin..");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }

            if (previousExp >= 0 && exp <= previousExp) {
                Bukkit.getLogger().severe("Invalid progression on exp table. Higher levels must have higher EXP.");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }

            experienceTable.put(level, exp);
            previousExp = exp;
        }
        Bukkit.getLogger().severe("Experience table has been loaded successfully.");
    }
}
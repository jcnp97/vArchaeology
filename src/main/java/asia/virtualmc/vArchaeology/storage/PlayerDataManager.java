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

public class PlayerDataManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataMap;
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
                    data.getExp(),
                    data.getLevel(),
                    data.getBreakChance(),
                    data.getGatherRate()
            );
        }
    }

    public void loadData(UUID uuid) {
        try (ResultSet rs = databaseManager.getPlayerData(uuid)) {
            if (rs.next()) {
                PlayerData data = new PlayerData(
                        rs.getString("playerName"),
                        rs.getInt("playerEXP"),
                        rs.getInt("playerLevel"),
                        rs.getDouble("breakChance"),
                        rs.getDouble("gatherRate")
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

    // Data Manipulation on HashMap
    public void updateExp(UUID uuid, int exp) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            data.setExp(exp);
        }
    }

    public void updateLevel(UUID uuid, int level) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            data.setLevel(level);
        }
    }
}


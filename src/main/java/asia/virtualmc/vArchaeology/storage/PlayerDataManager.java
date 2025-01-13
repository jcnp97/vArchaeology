// PlayerDataManager.class
package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final int MAX_LEVEL = 120;
    private static final long UPDATE_INTERVAL = 12000L;

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataMap;
    private final Map<Integer, Integer> experienceTable;

    public PlayerDataManager(@NotNull Main plugin, @NotNull DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerDataMap = new ConcurrentHashMap<>();
        this.experienceTable = new HashMap<>();

        loadExperienceTable();
        startUpdateTask();
    }

    @NotNull
    public Map<Integer, Integer> getExperienceTable() {
        return new HashMap<>(experienceTable);
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllData();
            }
        }.runTaskTimerAsynchronously(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    public void updateAllData() {
        playerDataMap.forEach((uuid, data) -> {
            try {
                databaseManager.savePlayerData(
                        uuid,
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
            } catch (Exception e) {
                plugin.getLogger().severe("[vArchaeology] Failed to save data for player " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void loadData(@NotNull UUID uuid) {
        try (ResultSet rs = databaseManager.getPlayerData(uuid)) {
            if (rs.next()) {
                PlayerData data = new PlayerData(
                        this,
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
            plugin.getLogger().severe("[vArchaeology] Failed to load data for player " + uuid + ": " + e.getMessage());
        }
    }

    public void unloadData(@NotNull UUID uuid) {
        playerDataMap.remove(uuid);
    }

    @Nullable
    public PlayerData getPlayerData(@NotNull UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void updateExp(@NotNull UUID uuid, int exp, @NotNull String param) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        switch (param.toLowerCase()) {
            case "add" -> data.addArchEXP(exp);
            case "sub" -> data.subtractArchEXP(exp);
            default -> data.setArchEXP(exp);
        }
    }

    public void updateLevel(@NotNull UUID uuid, int level, @NotNull String param) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        switch (param.toLowerCase()) {
            case "add" -> data.addArchLevel(level);
            case "sub" -> data.subtractArchLevel(level);
            default -> data.setArchLevel(level);
        }
    }

    public void loadExperienceTable() {
        File expTableFile = new File(plugin.getDataFolder(), "experience-table.yml");
        if (!expTableFile.exists()) {
            try {
                plugin.saveResource("experience-table.yml", false);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[vArchaeology] Experience table not found. Disabling the plugin..");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(expTableFile);
        int previousExp = -1;

        experienceTable.clear();
        for (String key : config.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String expString = config.getString(key, "0").replace(",", "");
                int exp = Integer.parseInt(expString);

                if (previousExp >= 0 && exp <= previousExp) {
                    throw new IllegalStateException("[vArchaeology] Invalid progression: Level " + level +
                            " has lower or equal EXP than previous level");
                }

                experienceTable.put(level, exp);
                previousExp = exp;
            } catch (NumberFormatException e) {
                plugin.getLogger().severe("[vArchaeology] Invalid number format in experience table at level " + key);
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            } catch (IllegalStateException e) {
                plugin.getLogger().severe(e.getMessage());
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        ConsoleMessageUtil.sendConsoleMessage("<#00FFA2>[vArchaeology] Experience table has been loaded.");
    }

//    public void checkAndApplyLevelUp(@NotNull PlayerData data) {
//        int currentLevel = data.getArchLevel();
//        int currentExp = data.getArchExp();
//        while (experienceTable.containsKey(currentLevel + 1) &&
//                currentExp >= experienceTable.get(currentLevel + 1) &&
//                currentLevel < MAX_LEVEL) {
//            data.addArchLevel(1);
//            currentLevel = data.getArchLevel();
//        }
//    }
}
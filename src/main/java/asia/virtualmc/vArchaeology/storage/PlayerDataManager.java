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
    private static final long UPDATE_INTERVAL = 12000L;
    private static final int MAX_EXP = 1_000_000_000;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 120;

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<Integer, Integer> experienceTable;
    private final Map<UUID, PlayerStats> playerStatsMap;

    public PlayerDataManager(@NotNull Main plugin, @NotNull DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.experienceTable = new HashMap<>();
        this.playerStatsMap = new ConcurrentHashMap<>();

        loadExperienceTable();
        startUpdateTask();
    }

    private static class PlayerStats {
        String name;
        int archEXP;
        int archLevel;
        int archApt;
        int archLuck;
        double archADP;
        double archXPMul;
        int archBonusXP;
        int blocksMined;
        int artefactsFound;
        int artefactsRestored;
        int treasuresFound;

        PlayerStats(String name, int archEXP, int archLevel, int archApt, int archLuck,
                    double archADP, double archXPMul, int archBonusXP,
                    int blocksMined, int artefactsFound, int artefactsRestored, int treasuresFound) {
            this.name = name;
            this.archEXP = archEXP;
            this.archLevel = archLevel;
            this.archApt = archApt;
            this.archLuck = archLuck;
            this.archADP = archADP;
            this.archXPMul = archXPMul;
            this.archBonusXP = archBonusXP;
            this.blocksMined = blocksMined;
            this.artefactsFound = artefactsFound;
            this.artefactsRestored = artefactsRestored;
            this.treasuresFound = treasuresFound;
        }
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
        playerStatsMap.forEach((uuid, stats) -> {
            try {
                databaseManager.savePlayerData(
                        uuid, stats.name, stats.archEXP, stats.archLevel,
                        stats.archApt, stats.archLuck, stats.archADP,
                        stats.archXPMul, stats.archBonusXP, stats.blocksMined,
                        stats.artefactsFound, stats.artefactsRestored, stats.treasuresFound
                );
            } catch (Exception e) {
                plugin.getLogger().severe("[vArchaeology] Failed to save data for player " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void loadData(@NotNull UUID uuid) {
        try (ResultSet rs = databaseManager.getPlayerData(uuid)) {
            if (rs.next()) {
                PlayerStats stats = new PlayerStats(
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
                playerStatsMap.put(uuid, stats);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[vArchaeology] Failed to load data for player " + uuid + ": " + e.getMessage());
        }
    }

    public void unloadData(@NotNull UUID uuid) {
        playerStatsMap.remove(uuid);
    }

    public void updateExp(@NotNull UUID uuid, int exp, @NotNull String param) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        switch (param.toLowerCase()) {
            case "add" -> {
                if (exp <= 0) return;
                stats.archEXP = Math.min(MAX_EXP, stats.archEXP + exp);
                checkAndApplyLevelUp(uuid);
            }
            case "sub" -> stats.archEXP = Math.max(0, stats.archEXP - exp);
            default -> stats.archEXP = Math.max(0, Math.min(exp, MAX_EXP));
        }
    }

    public void updateLevel(@NotNull UUID uuid, int level, @NotNull String param) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        switch (param.toLowerCase()) {
            case "add" -> {
                if (level <= 0) return;
                stats.archLevel = Math.min(MAX_LEVEL, stats.archLevel + level);
            }
            case "sub" -> {
                if (level <= 0) return;
                stats.archLevel = Math.max(MIN_LEVEL, stats.archLevel - level);
            }
            default -> stats.archLevel = Math.max(MIN_LEVEL, Math.min(level, MAX_LEVEL));
        }
    }

    private void checkAndApplyLevelUp(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        while (experienceTable.containsKey(stats.archLevel + 1) &&
                stats.archEXP >= experienceTable.get(stats.archLevel + 1) &&
                stats.archLevel < MAX_LEVEL) {
            stats.archLevel++;
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

    // Increment methods for achievements
    public void incrementBlocksMined(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.blocksMined++;
    }

    public void incrementArtefactsFound(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.artefactsFound++;
    }

    public void incrementArtefactsRestored(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.artefactsRestored++;
    }

    public void incrementTreasuresFound(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.treasuresFound++;
    }

    // Getter methods for player stats
    @Nullable
    public String getPlayerName(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.name : null;
    }

    public int getArchExp(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archEXP : 0;
    }

    public int getArchLevel(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archLevel : MIN_LEVEL;
    }

    public int getArchApt(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archApt : 0;
    }

    public int getArchBonusXP(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archBonusXP : 0;
    }

    public int getArchLuck(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archLuck : 0;
    }

    public double getArchADP(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archLuck : 0.0;
    }

    public double getArchXPMul(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archLuck : 0.0;
    }

    public int getBlocksMined(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.blocksMined : 0;
    }

    public int getArtefactsFound(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.artefactsFound : 0;
    }

    public int getArtefactsRestored(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.artefactsRestored : 0;
    }

    public int getTreasureFound(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.treasuresFound : 0;
    }
}
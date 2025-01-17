// PlayerDataManager.class
package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.utilities.ConsoleMessageUtil;
import asia.virtualmc.vArchaeology.utilities.BossBarUtil;

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
    private final PlayerDataDB playerDataDB;
    private final BossBarUtil bossBarUtil;
    private final ConfigManager configManager;
    private final Map<UUID, PlayerStats> playerStatsMap;

    public PlayerDataManager(@NotNull Main plugin, @NotNull PlayerDataDB playerDataDB, @NotNull BossBarUtil bossBarUtil, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.playerDataDB = playerDataDB;
        this.bossBarUtil = bossBarUtil;
        this.configManager = configManager;
        this.playerStatsMap = new ConcurrentHashMap<>();

        startUpdateTask();
    }

    private static class PlayerStats {
        String name;
        double archEXP;
        int archLevel;
        int archApt;
        int archLuck;
        double archADP;
        double archXPMul;
        int archBonusXP;
        int wisdomTrait;
        int charismaTrait;
        int karmaTrait;
        int dexterityTrait;
        int traitPoints;
        int talentPoints;

        PlayerStats(String name, double archEXP, int archLevel, int archApt, int archLuck, double archADP,
                    double archXPMul, int archBonusXP, int traitPoints, int talentPoints, int wisdomTrait,
                    int charismaTrait, int karmaTrait, int dexterityTrait
                    ) {
            this.name = name;
            this.archEXP = archEXP;
            this.archLevel = archLevel;
            this.archApt = archApt;
            this.archLuck = archLuck;
            this.archADP = archADP;
            this.archXPMul = archXPMul;
            this.archBonusXP = archBonusXP;
            this.wisdomTrait = wisdomTrait;
            this.charismaTrait = charismaTrait;
            this.karmaTrait = karmaTrait;
            this.dexterityTrait = dexterityTrait;
            this.traitPoints = traitPoints;
            this.talentPoints = talentPoints;
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

    public void updatePlayerData(UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) {
            return;
        }
        try {
            playerDataDB.savePlayerData(
                    uuid,
                    stats.name,
                    stats.archEXP,
                    stats.archLevel,
                    stats.archApt,
                    stats.archLuck,
                    stats.archADP,
                    stats.archXPMul,
                    stats.archBonusXP,
                    stats.traitPoints,
                    stats.talentPoints,
                    stats.wisdomTrait,
                    stats.charismaTrait,
                    stats.karmaTrait,
                    stats.dexterityTrait
            );
        } catch (Exception e) {
            plugin.getLogger().severe(
                    "[vArchaeology] Failed to save data for player " + uuid + ": " + e.getMessage()
            );
        }
    }

    public void updateAllData() {
        playerStatsMap.forEach((uuid, stats) -> {
            try {
                playerDataDB.savePlayerData(
                        uuid, stats.name, stats.archEXP, stats.archLevel,
                        stats.archApt, stats.archLuck, stats.archADP,
                        stats.archXPMul, stats.archBonusXP, stats.traitPoints,
                        stats.talentPoints, stats.wisdomTrait, stats.charismaTrait,
                        stats.karmaTrait, stats.dexterityTrait
                );
            } catch (Exception e) {
                plugin.getLogger().severe("[vArchaeology] Failed to save data for player " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void loadData(@NotNull UUID uuid) {
        try (ResultSet rs = playerDataDB.getPlayerData(uuid)) {
            if (rs.next()) {
                PlayerStats stats = new PlayerStats(
                        rs.getString("playerName"),
                        rs.getDouble("archEXP"),
                        rs.getInt("archLevel"),
                        rs.getInt("archApt"),
                        rs.getInt("archLuck"),
                        rs.getDouble("archADP"),
                        rs.getDouble("archXPMul"),
                        rs.getInt("archBonusXP"),
                        rs.getInt("traitPoints"),
                        rs.getInt("talentPoints"),
                        rs.getInt("wisdomTrait"),
                        rs.getInt("charismaTrait"),
                        rs.getInt("karmaTrait"),
                        rs.getInt("dexterityTrait")
                );
                playerStatsMap.put(uuid, stats);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[vArchaeology] Failed to load data for player " + uuid + ": " + e.getMessage());
        }
    }

    public void unloadData(@NotNull UUID uuid) {
        try {
            updatePlayerData(uuid);
            playerStatsMap.remove(uuid);
        } catch (Exception e) {
            plugin.getLogger().severe("[vArchaeology] Failed to save data for player " + uuid + ": " + e.getMessage());
        }
    }

    public void updateExp(@NotNull UUID uuid, double exp, @NotNull String param) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        // All experience received is rounded to two decimals
        exp = Math.round(exp * 100.0) / 100.0;

        switch (param.toLowerCase()) {
            case "add" -> {
                if (exp <= 0) return;
                stats.archEXP = Math.min(MAX_EXP, stats.archEXP + exp);
                if (stats.archLevel < 120) {
                    bossBarUtil.bossBarUpdate(uuid, exp, stats.archEXP, configManager.experienceTable.get(stats.archLevel + 1), stats.archLevel);
                    checkAndApplyLevelUp(uuid);
                }
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

    public void updateXPMul(@NotNull UUID uuid, double XPMul, @NotNull String param) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        switch (param.toLowerCase()) {
            case "add" -> {
                if (XPMul <= 0) return;
                stats.archXPMul += XPMul;
            }
            case "sub" -> {
                if (XPMul <= 0) return;
                stats.archXPMul = Math.max(0, stats.archXPMul - XPMul);
            }
            default -> {
                if (XPMul <= 0) return;
                stats.archXPMul = XPMul;
            }
        }
    }

    private void checkAndApplyLevelUp(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats == null) return;

        while (configManager.experienceTable.containsKey(stats.archLevel + 1) &&
                stats.archEXP >= configManager.experienceTable.get(stats.archLevel + 1) &&
                stats.archLevel < MAX_LEVEL) {
            stats.archLevel++;
        }
    }

    public void incrementTraitPoints(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.traitPoints++;
    }

    public void reduceTraitPoints(@NotNull UUID uuid, int value) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.traitPoints -= value;
    }

    public void incrementTalentPoints(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.talentPoints++;
    }

    public void reduceTalentPoints(@NotNull UUID uuid, int value) {
        PlayerStats stats = playerStatsMap.get(uuid);
        if (stats != null) stats.talentPoints -= value;
    }

    // Getter methods for player stats
    @Nullable
    public String getPlayerName(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.name : null;
    }

    public double getArchExp(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archEXP : 0.0;
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
        return stats != null ? stats.archADP : 0.0;
    }

    public double getArchXPMul(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.archXPMul : 1.0;
    }

    public int getTraitPoints(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.traitPoints : 0;
    }

    public int getTalentPoints(@NotNull UUID uuid) {
        PlayerStats stats = playerStatsMap.get(uuid);
        return stats != null ? stats.talentPoints : 0;
    }
}
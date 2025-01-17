package asia.virtualmc.vArchaeology.items;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;

import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RNGManager {
    private final Main plugin;
    private final Random random;
    private final ConfigManager configManager;
    private final Map<UUID, List<Integer>> playerDropTables;

    public RNGManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.random = new Random();
        this.configManager = configManager;
        this.playerDropTables = new HashMap<>();
    }

    public boolean hasDropTable(UUID playerId) {
        return playerDropTables.containsKey(playerId);
    }

    public void initializeDropTable(UUID playerId, int archLevel) {
        if (hasDropTable(playerId)) return;
        List<Integer> dropTable = new ArrayList<>();
        dropTable.add(configManager.commonWeight);
        if (archLevel >= 10) dropTable.add(configManager.uncommonWeight);
        if (archLevel >= 20) dropTable.add(configManager.rareWeight);
        if (archLevel >= 30) dropTable.add(configManager.uniqueWeight);
        if (archLevel >= 40) dropTable.add(configManager.specialWeight);
        if (archLevel >= 50) dropTable.add(configManager.mythicalWeight);
        if (archLevel >= 60) dropTable.add(configManager.exoticWeight);
        playerDropTables.put(playerId, dropTable);
    }

    public Integer rollDropTable(UUID playerId) {
        List<Integer> dropTable = playerDropTables.get(playerId);
        if (dropTable == null || dropTable.isEmpty()) return 0;

        int totalWeight = dropTable.stream().mapToInt(Integer::intValue).sum();
        int randomNumber = random.nextInt(totalWeight) + 1;

        int cumulativeWeight = 0;
        for (int i = 0; i < dropTable.size(); i++) {
            cumulativeWeight += dropTable.get(i);
            if (randomNumber <= cumulativeWeight) {
                return i + 1;
            }
        }
        return 0;
    }
}
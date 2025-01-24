package asia.virtualmc.vArchaeology.droptables;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.TalentTree;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemsDropTable {
    private final Main plugin;
    private final Random random;
    private final ConfigManager configManager;
    private final Map<UUID, List<Integer>> playerDropTables;
    private final TalentTree talentTree;

    public ItemsDropTable(@NotNull Main plugin,
                          @NotNull ConfigManager configManager,
                          @NotNull TalentTree talentTree) {
        this.plugin = plugin;
        this.random = new Random();
        this.configManager = configManager;
        this.talentTree = talentTree;
        this.playerDropTables = new HashMap<>();
    }

    public boolean hasDropTable(UUID uuid) {
        return playerDropTables.containsKey(uuid);
    }

    public void initializeDropTable(UUID uuid, int archLevel) {
        if (hasDropTable(uuid)) return;
        List<Integer> dropTable = new ArrayList<>();
        dropTable.add(configManager.commonWeight);
        if (archLevel >= 10) dropTable.add(configManager.uncommonWeight);
        if (archLevel >= 20) dropTable.add(configManager.rareWeight);
        if (archLevel >= 30) dropTable.add(configManager.uniqueWeight);
        if (archLevel >= 40) dropTable.add(configManager.specialWeight);
        if (archLevel >= 50) dropTable.add(configManager.mythicalWeight);
        if (archLevel >= 60) dropTable.add(configManager.exoticWeight);
        if (talentTree.getTalentLevel(uuid, 10) == 1) dropTable.set(6, configManager.exoticWeight + 3);
        playerDropTables.put(uuid, dropTable);
    }

    public void unloadData(UUID uuid) {
        if (!hasDropTable(uuid)) return;
        playerDropTables.remove(uuid);
    }

    public Integer rollDropTable(UUID uuid) {
        List<Integer> dropTable = playerDropTables.get(uuid);
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
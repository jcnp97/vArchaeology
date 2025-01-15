package asia.virtualmc.vArchaeology.items;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RNGManager {
    private final JavaPlugin plugin;
    private final Random random;
    private final Map<UUID, List<Integer>> playerDropTables;

    public RNGManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.playerDropTables = new HashMap<>();
    }

    public boolean hasDropTable(UUID playerId) {
        return playerDropTables.containsKey(playerId);
    }

    public void initializeDropTable(UUID playerId, int archLevel) {
        if (hasDropTable(playerId)) return;
        List<Integer> dropTable = new ArrayList<>();
        dropTable.add(80);
        if (archLevel > 10) dropTable.add(40);
        if (archLevel > 20) dropTable.add(25);
        if (archLevel > 30) dropTable.add(15);
        if (archLevel > 40) dropTable.add(8);
        if (archLevel > 50) dropTable.add(4);
        if (archLevel > 60) dropTable.add(1);
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
package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockBreakManager implements Listener {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final Random random;
    private int blockBreakChance;
    private final Map<Material, Integer> blocksList;

    public BlockBreakManager(@NotNull Main plugin, @NotNull PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.random = new Random();
        this.blocksList = new HashMap<>();

        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        loadConfiguration();
    }

    public void loadConfiguration() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Load blockBreakChance
        blockBreakChance = config.getInt("settings.blockBreakChance", 80);

        // Clear and reload blocksList
        blocksList.clear();
        ConfigurationSection blocksSection = config.getConfigurationSection("settings.blocksList");

        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    int expValue = blocksSection.getInt(key);
                    blocksList.put(material, expValue);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + key);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material material = block.getType();

        // Check if the broken block is in our configured list
        Integer expValue = blocksList.get(material);
        if (expValue == null) {
            return; // Not a configured block, let it break normally
        }

        // Cancel vanilla drops regardless of break chance
        event.setDropItems(false);

        // Process the break chance
        boolean shouldBreak = random.nextInt(100) < blockBreakChance;

        // If break chance fails, cancel the break but still give XP
        if (!shouldBreak) {
            event.setCancelled(true);
        }

        // Award XP to the player
        playerDataManager.updateExp(player.getUniqueId(), expValue, "add");
        playerDataManager.incrementBlocksMined(player.getUniqueId());

        // Increment blocks mined statistic
//        var playerData = playerDataManager.playerStatsMap.get(player.getUniqueId());
//        if (playerData != null) {
//            playerDataManager.incrementBlocksMined();
//        }
    }

    public void saveDefaultConfig() {
        FileConfiguration config = plugin.getConfig();

        // Set default values if they don't exist
        if (!config.contains("settings.blockBreakChance")) {
            config.set("settings.blockBreakChance", 80);
        }

        if (!config.contains("settings.blocksList.SAND")) {
            config.set("settings.blocksList.SAND", 1);
        }

        if (!config.contains("settings.blocksList.GRAVEL")) {
            config.set("settings.blocksList.GRAVEL", 2);
        }

        plugin.saveConfig();
    }
}

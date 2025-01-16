package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;

import asia.virtualmc.vArchaeology.storage.StatsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BlockBreakManager implements Listener {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;
    private final RNGManager rngManager;
    private final StatsManager statsManager;
    private final Random random;
    private final Map<Material, Integer> blocksList;

    public BlockBreakManager(@NotNull Main plugin, @NotNull PlayerDataManager playerDataManager, @NotNull ItemManager itemManager, @NotNull RNGManager rngManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
        this.rngManager = rngManager;
        this.statsManager = statsManager;
        this.random = new Random();
        this.blocksList = new HashMap<>();

        saveDefaultConfig();
        loadConfiguration();
    }

    public void loadConfiguration() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
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
        Location blockLocation = event.getBlock().getLocation();
        UUID uuid = player.getUniqueId();

        Integer expValue = blocksList.get(material);
        if (expValue == null) {
            return;
        }

        if (!rngManager.hasDropTable(uuid)) {
            rngManager.initializeDropTable(uuid, playerDataManager.getArchLevel(uuid));
        }

        event.setDropItems(false); // Cancel vanilla drops
        playerDataManager.updateExp(uuid, (double) expValue * (playerDataManager.getArchXPMul(uuid)), "add");
        statsManager.incrementStatistics(uuid, 8);
        itemManager.dropArchItem(uuid, rngManager.rollDropTable(uuid), blockLocation);
    }

    public void saveDefaultConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("settings.blocksList.SAND")) {
            config.set("settings.blocksList.SAND", 1);
        }

        if (!config.contains("settings.blocksList.GRAVEL")) {
            config.set("settings.blocksList.GRAVEL", 1);
        }

        if (!config.contains("settings.blocksList.GRASS_BLOCK")) {
            config.set("settings.blocksList.GRASS_BLOCK", 1);
        }

        if (!config.contains("settings.blocksList.DIRT")) {
            config.set("settings.blocksList.DIRT", 1);
        }

        if (!config.contains("settings.blocksList.CLAY_BLOCK")) {
            config.set("settings.blocksList.CLAY_BLOCK", 1);
        }
        plugin.saveConfig();
    }
}

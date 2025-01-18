package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;
import asia.virtualmc.vArchaeology.exp.EXPManager;

import asia.virtualmc.vArchaeology.storage.StatsManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakListener implements Listener {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;
    private final RNGManager rngManager;
    private final StatsManager statsManager;
    private final EXPManager expManager;
    private final Map<Material, Integer> blocksList;
    private final Map<UUID, Long> adpCooldowns;
    private static final long ADP_COOLDOWN = 60_000;

    public BlockBreakListener(@NotNull Main plugin, @NotNull PlayerDataManager playerDataManager, @NotNull ItemManager itemManager, @NotNull RNGManager rngManager, StatsManager statsManager, EXPManager expManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
        this.rngManager = rngManager;
        this.statsManager = statsManager;
        this.expManager = expManager;
        this.blocksList = new HashMap<>();
        this.adpCooldowns = new HashMap<>();

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
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        UUID playerUUID = player.getUniqueId();

        if (!itemManager.isArchTool(mainHandItem)) {
            return;
        }

        if (itemManager.getDurability(mainHandItem) <= 10) {
            event.setCancelled(true);
            player.sendMessage("§cYour tool's durability is too low to break this block!");
            return;
        }

        if (itemManager.getRequiredLevel(mainHandItem) > playerDataManager.getArchLevel(playerUUID)) {
            event.setCancelled(true);
            player.sendMessage("§cYou do not have the required level to use this tool!");
            return;
        }

        Block block = event.getBlock();
        Material material = block.getType();
        Integer expValue = blocksList.get(material);
        if (expValue == null) {
            return;
        }
        Location blockLocation = event.getBlock().getLocation();
        UUID uuid = player.getUniqueId();

        if (!rngManager.hasDropTable(uuid)) {
            rngManager.initializeDropTable(uuid, playerDataManager.getArchLevel(uuid));
        }

        event.setDropItems(false);
        playerDataManager.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue), "add");
        statsManager.incrementStatistics(uuid, 8);
        itemManager.dropArchItem(uuid, rngManager.rollDropTable(uuid), blockLocation);

        // Artefact Discovery Progress
        if (!canProgressAD(playerUUID)) {
            return;
        }
    }

    public boolean canProgressAD(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastUsage = adpCooldowns.get(playerUUID);

        if (lastUsage == null || currentTime - lastUsage >= ADP_COOLDOWN) {
            adpCooldowns.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }

    public void cleanupExpiredADPCooldowns() {
        long currentTime = System.currentTimeMillis();
        adpCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() >= ADP_COOLDOWN);
    }
}

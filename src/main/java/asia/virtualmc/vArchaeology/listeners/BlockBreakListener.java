package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockBreakListener implements Listener {
    private final Main plugin;
    private final PlayerData playerData;
    private final ItemManager itemManager;
    private final RNGManager rngManager;
    private final Statistics statistics;
    private final EXPManager expManager;
    private final ConfigManager configManager;
    private final ItemEquipListener itemEquipListener;
    private final EffectsUtil effectsUtil;
    private final Map<Material, Integer> blocksList;
    private final Map<UUID, Long> adpCooldowns;
    private final Random random;
    private static final long ADP_COOLDOWN = 60_000;

    public BlockBreakListener(@NotNull Main plugin,
                              @NotNull PlayerData playerData,
                              @NotNull ItemManager itemManager,
                              @NotNull RNGManager rngManager,
                              @NotNull Statistics statistics,
                              EXPManager expManager,
                              ConfigManager configManager,
                              ItemEquipListener itemEquipListener,
                              EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.itemManager = itemManager;
        this.rngManager = rngManager;
        this.statistics = statistics;
        this.expManager = expManager;
        this.configManager = configManager;
        this.itemEquipListener = itemEquipListener;
        this.effectsUtil = effectsUtil;
        this.blocksList = new HashMap<>(configManager.loadBlocksList());
        this.adpCooldowns = new HashMap<>();
        this.random = new Random();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (!itemManager.isArchTool(mainHandItem)) {
            return;
        }

        if (!canBreakBlocks(player, playerUUID, mainHandItem)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        Material material = block.getType();
        Integer expValue = blocksList.get(material);
        if (expValue == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        event.setDropItems(false);
        statistics.incrementStatistics(uuid, 9);

        // Material Drops
        if (random.nextDouble() < itemEquipListener.getGatherValue(uuid) / 100) {
            Location blockLocation = event.getBlock().getLocation();
            int dropTable = rngManager.rollDropTable(uuid);
            // item drop
            itemManager.dropArchItem(uuid, dropTable, blockLocation);
            // experience
            playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue) + expManager.getTotalMaterialGetEXP(uuid, dropTable), "add");
        } else {
            playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue), "add");
        }

        // Artefact Discovery Progress
//        if (canProgressAD(playerUUID)) {
//            playerData.addArtefactDiscovery(uuid, itemEquipListener.getAdbValue(uuid));
//        }
        double adbAdd = itemEquipListener.getAdbValue(uuid);
        playerData.addArtefactDiscovery(uuid, adbAdd);
        double adbProgress = playerData.getArchADP(uuid);

        effectsUtil.sendADBProgressBarTitle(uuid, adbProgress / 100.0, adbAdd);
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

    public boolean canBreakBlocks(Player player, UUID uuid, ItemStack mainHandItem) {
        if (itemManager.getDurability(mainHandItem) <= 10) {
            player.sendMessage("§cYour tool's durability is too low to break this block!");
            return false;
        }

        if (itemManager.getRequiredLevel(mainHandItem) > playerData.getArchLevel(uuid)) {
            player.sendMessage("§cYou do not have the required level to use this tool!");
            return false;
        }

        return true;
    }
}

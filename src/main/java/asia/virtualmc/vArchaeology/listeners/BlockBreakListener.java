package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.CustomCharms;
import asia.virtualmc.vArchaeology.items.CustomItems;
import asia.virtualmc.vArchaeology.items.CustomTools;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.droptables.ItemsDropTable;
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
    private final CustomTools customTools;
    private final CustomCharms customCharms;
    private final CustomItems customItems;
    private final ItemsDropTable itemsDropTable;
    private final Statistics statistics;
    private final CollectionLog collectionLog;
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
                              @NotNull CustomItems customItems,
                              @NotNull CustomTools customTools,
                              @NotNull CustomCharms customCharms,
                              @NotNull ItemsDropTable itemsDropTable,
                              @NotNull Statistics statistics,
                              @NotNull CollectionLog collectionLog,
                              EXPManager expManager,
                              ConfigManager configManager,
                              ItemEquipListener itemEquipListener,
                              EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.customItems = customItems;
        this.customTools = customTools;
        this.customCharms = customCharms;
        this.itemsDropTable = itemsDropTable;
        this.statistics = statistics;
        this.collectionLog = collectionLog;
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

        if (!customTools.isArchTool(mainHandItem)) {
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

        if (!itemEquipListener.hasToolData(uuid)) {
            itemEquipListener.addPlayerData(player, mainHandItem);
        }

        // Material Drops
        if (random.nextDouble() < itemEquipListener.getGatherValue(uuid) / 100) {
            Location blockLocation = event.getBlock().getLocation();
            int dropTable = calculateDropTable(uuid, player);
            // item drop
            customItems.dropArchItem(uuid, dropTable, blockLocation);
            // experience
            playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue) + expManager.getTotalMaterialGetEXP(uuid, dropTable), "add");
            // statistics
            collectionLog.incrementCollection(uuid, dropTable);
        } else {
            playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue), "add");
        }

        // Tier 99 Passive
        if (itemEquipListener.hasTier99Value(uuid)) {
            if (random.nextInt(10) < 1) {
                int dropTable = calculateDropTable(uuid, player);
                customItems.dropArchItem(uuid, dropTable, event.getBlock().getLocation());
                collectionLog.incrementCollection(uuid, dropTable);
                effectsUtil.sendPlayerMessage(uuid, "<green>Your Time-Space Convergence has been activated.");
            }

            if (random.nextInt(100) < 1) {
                playerData.addArtefactDiscovery(uuid, 1.0);
                effectsUtil.sendADBProgressBarTitle(uuid, playerData.getArchADP(uuid) / 100.0, 1.0);
                effectsUtil.sendPlayerMessage(uuid, "<green>Your Chronal Focus has been activated.");
            }
        }

        // Artefact Discovery Progress
        if (canProgressAD(playerUUID)) {
            double adbAdd = itemEquipListener.getAdbValue(uuid);
            playerData.addArtefactDiscovery(uuid, adbAdd);
            double adbProgress = playerData.getArchADP(uuid);
            effectsUtil.sendADBProgressBarTitle(uuid, adbProgress / 100.0, adbAdd);
        }
    }

    private int calculateDropTable(UUID uuid, Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        int charmID = customCharms.getCharmID(offHandItem);

        if (charmID > 0) {
            offHandItem.setAmount(offHandItem.getAmount() - 1);
            return charmID;
        }
        return itemsDropTable.rollDropTable(uuid);
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
        if (customTools.getDurability(mainHandItem) <= 10) {
            player.sendMessage("§cYour tool's durability is too low to break this block!");
            return false;
        }

        if (customTools.getRequiredLevel(mainHandItem) > playerData.getArchLevel(uuid)) {
            player.sendMessage("§cYou do not have the required level to use this tool!");
            return false;
        }

        return true;
    }
}

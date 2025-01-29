package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.droptables.ItemsDropTable;
import asia.virtualmc.vArchaeology.items.CustomTools;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.TalentTree;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemEquipListener implements Listener {

    private record ToolData(double gather, double adb, boolean hasTier99) {}
    private final Main plugin;
    private final CustomTools customTools;
    private final TalentTree talentTree;
    private final PlayerData playerData;
    private final ItemsDropTable itemsDropTable;
    private final ConfigManager configManager;
    private final Map<UUID, ToolData> toolDataMap;
    private final NamespacedKey gatherKey;
    private final NamespacedKey adbKey;

    public ItemEquipListener(Main plugin,
                             CustomTools customTools,
                             PlayerData playerData,
                             TalentTree talentTree,
                             ItemsDropTable itemsDropTable,
                             ConfigManager configManager) {
        this.plugin = plugin;
        this.customTools = customTools;
        this.talentTree = talentTree;
        this.playerData = playerData;
        this.itemsDropTable = itemsDropTable;
        this.configManager = configManager;
        this.toolDataMap = new ConcurrentHashMap<>();
        this.gatherKey = new NamespacedKey(plugin, "varch_gather");
        this.adbKey = new NamespacedKey(plugin, "varch_adb");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                unloadToolData(uuid);

                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                if (!isValidItem(mainHandItem)) {
                    return;
                }

                addPlayerData(player, mainHandItem);
            }
        }.runTaskLater(plugin, 5L);
    }

    private boolean isValidItem(ItemStack item) {
        return item != null
                && item.getType() != Material.AIR
                && customTools.isArchTool(item)
                && item.hasItemMeta();
    }

    public void addPlayerData(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        double gatherRate = calculateGatherRate(uuid, pdc);
        double adbProgress = calculateADB(uuid, pdc);

        if (gatherRate > 0 || adbProgress > 0) {
            toolDataMap.put(uuid, new ToolData(gatherRate, adbProgress, hasTier99(item)));
        }

        if (!itemsDropTable.hasDropTable(uuid)) {
            itemsDropTable.initializeDropTable(uuid, playerData.getArchLevel(uuid));
        }
    }

    private double calculateGatherRate(UUID uuid, PersistentDataContainer pdc) {
        // Tool Gather Rate
        double gatherRate = pdc.getOrDefault(gatherKey, PersistentDataType.DOUBLE, 0.0);
        // Talent ID 3
        gatherRate += talentTree.getTalentLevel(uuid, 3) * 0.1;
        // Karma Trait
        gatherRate += playerData.getKarmaTrait(uuid) * configManager.karmaEffects[0];
        // Karma Trait (Max level bonus)
        gatherRate += configManager.karmaEffects[3];

        return gatherRate;
    }

    private double calculateADB(UUID uuid, PersistentDataContainer pdc) {
        // Tool ADB
        double adbProgress = pdc.getOrDefault(adbKey, PersistentDataType.DOUBLE, 0.0);
        // Talent ID 8
        adbProgress += talentTree.getTalentLevel(uuid, 8) * 0.01;
        // Talent ID 17
        adbProgress += talentTree.getTalentLevel(uuid, 17) * 0.15;
        // Dexterity Trait
        adbProgress += playerData.getDexterityTrait(uuid) * configManager.dexterityEffects[0];

        if (playerData.getDexterityTrait(uuid) >= 50) {
            adbProgress += configManager.dexterityEffects[3];
        }

        return adbProgress;
    }

    private boolean hasTier99(ItemStack mainHandItem) {
        return customTools.getToolId(mainHandItem) == 10;
    }

    public void unloadToolData(UUID uuid) {
        toolDataMap.remove(uuid);
    }

    public double getGatherValue(UUID uuid) {
        ToolData data = toolDataMap.get(uuid);
        return (data == null) ? 0.0 : data.gather();
    }

    public double getAdbValue(UUID uuid) {
        ToolData data = toolDataMap.get(uuid);
        return (data == null) ? 0.0 : data.adb();
    }

    public boolean hasTier99Value(UUID uuid) {
        ToolData data = toolDataMap.get(uuid);
        return data != null && data.hasTier99();
    }

    public boolean hasToolData(UUID uuid) {
        return toolDataMap.containsKey(uuid);
    }
}

package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.items.RNGManager;
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

    private record GatherAdbData(double gather, double adb) {}
    private final Main plugin;
    private final ItemManager itemManager;
    private final TalentTree talentTree;
    private final PlayerData playerData;
    private final RNGManager rngManager;
    private final Map<UUID, GatherAdbData> gatherAdbMap;
    private final NamespacedKey gatherKey;
    private final NamespacedKey adbKey;

    public ItemEquipListener(Main plugin, ItemManager itemManager, PlayerData playerData,
                             TalentTree talentTree, RNGManager rngManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.talentTree = talentTree;
        this.playerData = playerData;
        this.rngManager = rngManager;
        this.gatherAdbMap = new ConcurrentHashMap<>();
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

                unloadData(uuid);

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
                && itemManager.isArchTool(item)
                && item.hasItemMeta();
    }

    public void addPlayerData(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        double gatherRate = calculateGatherRate(uuid, pdc);
        double adbProgress = calculateADB(uuid, pdc);

        if (gatherRate > 0 || adbProgress > 0) {
            gatherAdbMap.put(uuid, new GatherAdbData(gatherRate, adbProgress));
        }

        if (!rngManager.hasDropTable(uuid)) {
            rngManager.initializeDropTable(uuid, playerData.getArchLevel(uuid));
        }
    }

    private double calculateGatherRate(UUID uuid, PersistentDataContainer pdc) {
        // Tool Gather Rate
        double gatherRate = pdc.getOrDefault(gatherKey, PersistentDataType.DOUBLE, 0.0);
        // Talent ID 3
        gatherRate += talentTree.getTalentLevel(uuid, 3) * 0.1;
        // Karma Trait
        gatherRate += playerData.getKarmaTrait(uuid) * 0.05;

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
        adbProgress += playerData.getDexterityTrait(uuid) * 0.005;

        return adbProgress;
    }

    public void unloadData(UUID uuid) {
        gatherAdbMap.remove(uuid);
    }

    public double getGatherValue(UUID uuid) {
        GatherAdbData data = gatherAdbMap.get(uuid);
        return (data == null) ? 0.0 : data.gather();
    }

    public double getAdbValue(UUID uuid) {
        GatherAdbData data = gatherAdbMap.get(uuid);
        return (data == null) ? 0.0 : data.adb();
    }

    public boolean hasGatherAndADBData(UUID uuid) {
        return gatherAdbMap.containsKey(uuid);
    }
}

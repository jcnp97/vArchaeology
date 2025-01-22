package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.items.ItemManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.TalentTree;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class ItemEquipListener implements Listener {
    private final Main plugin;
    private final ItemManager itemManager;
    private final TalentTree talentTree;
    private final PlayerData playerData;
    private final Map<UUID, Double> gatherMap;
    private final Map<UUID, Double> adbMap;
    private final NamespacedKey gatherKey;
    private final NamespacedKey adbKey;

    public ItemEquipListener(Main plugin, ItemManager itemManager, PlayerData playerData, TalentTree talentTree) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.talentTree = talentTree;
        this.playerData = playerData;
        this.gatherMap = new ConcurrentHashMap<>();
        this.adbMap = new ConcurrentHashMap<>();
        this.gatherKey = new NamespacedKey(plugin, "varch_gather");
        this.adbKey = new NamespacedKey(plugin, "varch_adb");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        unloadData(uuid);

        if (!isValidItem(mainHandItem)) {
            return;
        }

        calculateAndUpdateValues(player, mainHandItem);
    }

    private boolean isValidItem(ItemStack item) {
        return item != null &&
                item.getType() != Material.AIR &&
                itemManager.isArchTool(item) &&
                item.hasItemMeta();
    }

    private void calculateAndUpdateValues(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        double gatherValue = pdc.getOrDefault(gatherKey, PersistentDataType.DOUBLE, 0.0);
        double adbValue = pdc.getOrDefault(adbKey, PersistentDataType.DOUBLE, 0.0);

        double totalGatherBonus = calculateGatherBonus(uuid);
        double totalAdbBonus = calculateAdbBonus(uuid);

        if (gatherValue > 0) {
            gatherMap.put(uuid, gatherValue + totalGatherBonus);
        }
        if (adbValue > 0) {
            adbMap.put(uuid, adbValue + totalAdbBonus);
        }
    }

    private double calculateGatherBonus(UUID uuid) {
        return (talentTree.getTalentLevel(uuid, 3) * 0.1) +
                (playerData.getKarmaTrait(uuid) * 0.05);
    }

    private double calculateAdbBonus(UUID uuid) {
        return (talentTree.getTalentLevel(uuid, 8) * 0.01) +
                (talentTree.getTalentLevel(uuid, 17) * 0.15) +
                (playerData.getDexterityTrait(uuid) * 0.005);
    }

    public void unloadData(UUID uuid) {
        gatherMap.remove(uuid);
        adbMap.remove(uuid);
    }

    public Double getGatherValue(UUID uuid) {
        return gatherMap.getOrDefault(uuid, 0.0);
    }

    public Double getAdbValue(UUID uuid) {
        return adbMap.getOrDefault(uuid, 0.0);
    }

    public void setGatherValue(UUID uuid, double value) {
        gatherMap.put(uuid, value);
    }

    public void setAdbValue(UUID uuid, double value) {
        adbMap.put(uuid, value);
    }

    public void incrementGatherValue(UUID uuid, double increment) {
        gatherMap.compute(uuid, (key, value) -> (value == null ? 0.0 : value) + increment);
    }

    public void incrementAdbValue(UUID uuid, double increment) {
        adbMap.compute(uuid, (key, value) -> (value == null ? 0.0 : value) + increment);
    }
}
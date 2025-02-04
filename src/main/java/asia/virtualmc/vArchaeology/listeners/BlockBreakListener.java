package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.items.CraftingMaterials;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final CraftingMaterials craftingMaterials;
    private final Map<Material, Integer> blocksList;
    private final Map<UUID, Long> adpCooldowns;
    private record TraitData(double extraRoll, double nextTier, double doubleADP, double addADP) {}
    private final Map<UUID, TraitData> traitDataMap;
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
                              @NotNull EXPManager expManager,
                              @NotNull ConfigManager configManager,
                              @NotNull ItemEquipListener itemEquipListener,
                              @NotNull EffectsUtil effectsUtil,
                              @NotNull CraftingMaterials craftingMaterials) {
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
        this.craftingMaterials = craftingMaterials;
        this.blocksList = new HashMap<>(configManager.loadBlocksList());
        this.adpCooldowns = new HashMap<>();
        this.traitDataMap = new ConcurrentHashMap<>();
        this.random = new Random();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (!customTools.isArchTool(mainHandItem)) {
            return;
        }

        if (!canBreakBlocks(player, uuid, mainHandItem)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        Material material = block.getType();
        Integer expValue = blocksList.get(material);
        if (expValue == null) {
            return;
        }

        event.setDropItems(false);
        statistics.incrementStatistics(uuid, 9);

        if (!itemEquipListener.hasToolData(uuid)) {
            itemEquipListener.addPlayerData(player, mainHandItem);
        }

        // Karma Trait - Two Drops & Next-Tier
        // Dexterity Trait - Double Artefact Discovery Progress
        if (!traitDataMap.containsKey(uuid)) {
            addTraitData(player);
        }

        // Material Drops
        if (random.nextDouble() < itemEquipListener.getGatherValue(uuid) / 100) {
            Location blockLocation = event.getBlock().getLocation();
            giveArchMaterialDrop(player, blockLocation, expValue, true);
        } else {
            playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue), "add");
        }

        // Tier 99 Passive
        if (itemEquipListener.hasTier99Value(uuid)) {
            tier99Passive(player);
        }

        // Artefact Discovery Progress
        if (canProgressAD(uuid)) {
            addArtefactProgress(uuid);
            if (random.nextDouble() < getDoubleADP(uuid) / 100) {
                addArtefactProgress(uuid);
                effectsUtil.sendPlayerMessage(uuid, "<green>Your Cosmic Focus (Dexterity Trait) has doubled your Artefact Progress gain.");
            }
        }

        // Dexterity Bonus
        if (random.nextDouble() < getAddADP(uuid) / 100) {
            playerData.addArtefactDiscovery(uuid, 0.1);
            effectsUtil.sendADBProgressBarTitle(uuid, playerData.getArchADP(uuid) / 100.0, 0.1);
        }

        if (random.nextInt(1, 2501) == 1) {
            String blockType = material.name();
            switch (blockType) {
                case "SAND" -> giveCraftingMaterial(player, 2);
                case "RED_SAND" -> giveCraftingMaterial(player, 3);
                case "SOUL_SAND" -> giveCraftingMaterial(player, 4);
                case "DIRT" -> giveCraftingMaterial(player, 5);
                case "GRAVEL" -> giveCraftingMaterial(player, 6);
            }
        }
    }

    private void giveArchMaterialDrop(Player player, Location blockLocation, int expValue, boolean canTriggerPassive) {
        UUID uuid = player.getUniqueId();
        int dropTable = calculateDropTable(uuid, player);
        double randomNumber = random.nextDouble();

        if ((randomNumber < getHigherTier(uuid) / 100) && canTriggerPassive) {
            if (dropTable < 7) {
                dropTable += 1;
            }
        }
        // item drop
        customItems.dropArchItem(uuid, dropTable, blockLocation);
        // experience
        playerData.updateExp(uuid, expManager.getTotalBlockBreakEXP(uuid, expValue) + expManager.getTotalMaterialGetEXP(uuid, dropTable), "add");
        // statistics
        collectionLog.incrementCollection(uuid, dropTable);

        if ((randomNumber < getTwoDrops(uuid) / 100) && canTriggerPassive) {
            giveArchMaterialDrop(player, blockLocation, expValue, false);
        }
    }

    private void addArtefactProgress(UUID uuid) {
        double adbAdd = itemEquipListener.getAdbValue(uuid);
        playerData.addArtefactDiscovery(uuid, adbAdd);
        double adbProgress = playerData.getArchADP(uuid);
        effectsUtil.sendADBProgressBarTitle(uuid, adbProgress / 100.0, adbAdd);
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

    private boolean canProgressAD(UUID playerUUID) {
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

    private boolean canBreakBlocks(Player player, UUID uuid, ItemStack mainHandItem) {
        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            player.sendMessage("§cYour haste buff prevents you from breaking this block.");
            return false;
        }

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

    private void tier99Passive(Player player) {
        UUID uuid = player.getUniqueId();
        if (random.nextInt(10) < 1) {
            int dropTable = calculateDropTable(uuid, player);
            customItems.giveArchItem(uuid, dropTable, 1);
            collectionLog.incrementCollection(uuid, dropTable);
            effectsUtil.sendPlayerMessage(uuid, "<green>Your Time-Space Convergence has been activated.");
        }

        if (random.nextInt(100) < 1) {
            playerData.addArtefactDiscovery(uuid, 1.0);
            effectsUtil.sendADBProgressBarTitle(uuid, playerData.getArchADP(uuid) / 100.0, 1.0);
            effectsUtil.sendPlayerMessage(uuid, "<green>Your Chronal Focus has been activated.");
        }
    }

    private void addTraitData(Player player) {
        UUID uuid = player.getUniqueId();
        int karmaLevel = playerData.getKarmaTrait(uuid);
        int dexterityLevel = playerData.getDexterityTrait(uuid);

        double extraRoll = (double) karmaLevel * configManager.karmaEffects[1];
        double nextTierRoll = (double) karmaLevel * configManager.karmaEffects[2];
        double doubleADP = (double) dexterityLevel * configManager.dexterityEffects[1];
        double addADP = (double) dexterityLevel * configManager.dexterityEffects[2];

        traitDataMap.put(uuid, new TraitData(extraRoll, nextTierRoll, doubleADP, addADP));
    }

    public void unloadTraitData(UUID uuid) {
        if (!traitDataMap.containsKey(uuid)) return;
        traitDataMap.remove(uuid);
    }

    private double getTwoDrops(UUID uuid) {
        TraitData data = traitDataMap.get(uuid);
        return (data == null) ? 0.0 : data.extraRoll();
    }

    private double getHigherTier(UUID uuid) {
        TraitData data = traitDataMap.get(uuid);
        return (data == null) ? 0.0 : data.nextTier();
    }

    private double getDoubleADP(UUID uuid) {
        TraitData data = traitDataMap.get(uuid);
        return (data == null) ? 0.0 : data.doubleADP();
    }

    private double getAddADP(UUID uuid) {
        TraitData data = traitDataMap.get(uuid);
        return (data == null) ? 0.0 : data.addADP();
    }

    private void giveCraftingMaterial(Player player, int matID) {
        craftingMaterials.giveCraftingMaterial(player.getUniqueId(), matID, 1);
        effectsUtil.sendBroadcastMessage("<dark_red>" + player.getName() +
                " <gold>has obtained " + EffectsUtil.convertLegacy(craftingMaterials.getDisplayName(matID)) +
                "<gold> from Archaeology!"
        );

    }
}

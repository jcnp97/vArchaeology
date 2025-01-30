package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.logs.SellLog;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.storage.TalentTree;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.milkbowl.vault.economy.Economy;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SellGUI implements Listener {
    private record SellMultiplierData(double drops, double artefacts, double taxes) {}
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final PlayerData playerData;
    private final ConfigManager configManager;
    private final TalentTree talentTree;
    private final Statistics statistics;
    private final SellLog sellLog;
    private final Economy economy;
    private final NamespacedKey varchItemKey;
    private static final long GUI_COOLDOWN = 500;
    private final Map<UUID, Long> lastGuiOpen = new ConcurrentHashMap<>();
    private final Map<UUID, SellMultiplierData> sellDataMap = new ConcurrentHashMap<>();

    public SellGUI(Main plugin,
                   EffectsUtil effectsUtil,
                   PlayerData playerData,
                   ConfigManager configManager,
                   TalentTree talentTree,
                   Statistics statistics,
                   SellLog sellLog) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.playerData = playerData;
        this.configManager = configManager;
        this.talentTree = talentTree;
        this.statistics = statistics;
        this.sellLog = sellLog;
        this.economy = Main.getEconomy();
        this.varchItemKey = new NamespacedKey(plugin, "varch_item");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openSellGUI(Player player) {
        long currentTime = System.currentTimeMillis();
        UUID playerUUID = player.getUniqueId();

        if (lastGuiOpen.containsKey(playerUUID) &&
                currentTime - lastGuiOpen.get(playerUUID) < GUI_COOLDOWN) {
            player.sendMessage("§cPlease wait before opening the sell GUI again!");
            return;
        }

        lastGuiOpen.put(playerUUID, currentTime);
        double initialValue = calculateInventoryValue(player);

        ChestGui gui = new ChestGui(5, "§f\uE0F1\uE0F1\uE053\uD833\uDEAF");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = createStaticPane(player, initialValue);
        OutlinePane itemPane = createItemPane(player);

        gui.addPane(staticPane);
        gui.addPane(itemPane);
        gui.show(player);
    }

    private StaticPane createStaticPane(Player player, double initialValue) {
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);

        // Add sell buttons
        for (int x = 1; x <= 3; x++) {
            ItemStack sellButton = createSellButton(initialValue);
            GuiItem guiItem = new GuiItem(sellButton, event -> processSellAction(player, initialValue));
            staticPane.addItem(guiItem, x, 4);
        }

        // Add close buttons
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 4);
        }

        return staticPane;
    }

    private void processSellAction(Player player, double initialValue) {
        double currentValue = calculateInventoryValue(player);

        if (currentValue != initialValue) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        try {
            if (currentValue > 0) {
                UUID uuid = player.getUniqueId();
                double taxesPaid = taxDeduction(player, currentValue);
                sellItems(player, currentValue);
                statistics.addStatistics(uuid, 20, (int) currentValue);
                statistics.addStatistics(uuid, 21, (int) taxesPaid);
                effectsUtil.sendPlayerMessage(uuid, configManager.pluginPrefix + "<#00FFA2>You have sold your items for <gold>$" + currentValue);
                effectsUtil.sendPlayerMessage(uuid, configManager.pluginPrefix + "<gold>$" + taxesPaid + " <red>has been deducted from your balance.");
                effectsUtil.playSound(player, "minecraft:cozyvanilla.sell_confirmed", Sound.Source.PLAYER, 1.0f, 1.0f);
            } else {
                player.sendMessage("§cNo sellable items found in your inventory!");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing your sale. Please try again.");
            plugin.getLogger().severe("Error processing sale for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        player.closeInventory();
    }

    private ItemStack createSellButton(double totalValue) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aSell Items");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§7Current total: §2$" + totalValue,
                    "§eClick to sell all sellable items!"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private OutlinePane createItemPane(Player player) {
        OutlinePane itemPane = new OutlinePane(0, 0, 9, 4);
        Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && isSellable(item))
                .map(ItemStack::clone)
                .forEach(itemCopy -> itemPane.addItem(new GuiItem(itemCopy)));
        return itemPane;
    }

    private boolean isSellable(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return getCustomId(item) != null;
    }

    private Integer getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer customID = pdc.get(varchItemKey, PersistentDataType.INTEGER);
        return (customID != null && customID >= 1 && customID <= 7) ? customID : null;
    }

    private double getItemValue(ItemStack item, UUID uuid) {
        Integer customID = getCustomId(item);
        if (customID == null) return 0;

        return switch (customID) {
            case 1 -> configManager.dropBasePrice[0] * getDropsData(uuid);
            case 2 -> configManager.dropBasePrice[1] * getDropsData(uuid);
            case 3 -> configManager.dropBasePrice[2] * getDropsData(uuid);
            case 4 -> configManager.dropBasePrice[3] * getDropsData(uuid);
            case 5 -> configManager.dropBasePrice[4] * getDropsData(uuid);
            case 6 -> configManager.dropBasePrice[5] * getDropsData(uuid);
            case 7 -> configManager.dropBasePrice[6] * getDropsData(uuid);
            default -> 0.0;
        };
    }

    private String getItemRarity(ItemStack item) {
        Integer customID = getCustomId(item);
        if (customID == null) return "null";

        return switch (customID) {
            case 1 -> "Common";
            case 2 -> "Uncommon";
            case 3 -> "Rare";
            case 4 -> "Unique";
            case 5 -> "Special";
            case 6 -> "Mythical";
            case 7 -> "Exotic";
            default -> "null";
        };
    }

    private double calculateInventoryValue(Player player) {
        UUID uuid = player.getUniqueId();
        double sum = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && isSellable(item))
                .mapToDouble(item -> getItemValue(item, uuid) * item.getAmount())
                .sum();

        return Math.round(sum * 100.0) / 100.0;
    }

    private void sellItems(Player player, double totalValue) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (isSellable(stack)) {
                sellLog.logTransaction(player.getName(), getItemRarity(stack), stack.getAmount());
                player.getInventory().setItem(i, null);
            }
        }
        if (economy != null && totalValue > 0) {
            economy.depositPlayer(player, totalValue);
        }
    }

    private double taxDeduction(Player player, double totalValue) {
        UUID uuid = player.getUniqueId();
        double taxPaid = Math.round(totalValue * getTaxData(uuid) * 100.0) / 100.0;
        economy.withdrawPlayer(player, taxPaid);
        return taxPaid;
    }

    public void loadPlayerSellData(UUID uuid) {
        if (hasSellData(uuid)) return;

        double dropsBaseMultiplier = calcDropsSellMultiplier(uuid);
        double artefactsBaseMultiplier = calcArtefactsSellMultiplier(uuid);
        double taxPercentage = (Math.min(statistics.getStatistics(uuid, 1), 50) * 1.2) / 100.0;

        sellDataMap.put(uuid, new SellMultiplierData(dropsBaseMultiplier, artefactsBaseMultiplier, 0.75 - taxPercentage));
    }

    private double calcDropsSellMultiplier(UUID uuid) {
        double baseMultiplier = 100.0;
        int charismaLevel = playerData.getCharismaTrait(uuid);

        // Charisma Trait Bonus
        baseMultiplier += (double) charismaLevel * configManager.charismaEffects[0];
        // Talent ID 5
        baseMultiplier += (double) talentTree.getTalentLevel(uuid, 5) * 2;

        return baseMultiplier / 100.0;
    }

    private double calcArtefactsSellMultiplier(UUID uuid) {
        double baseMultiplier = 100.0;
        int charismaLevel = playerData.getCharismaTrait(uuid);

        // Charisma Trait Bonus
        baseMultiplier += (double) charismaLevel * configManager.charismaEffects[1];
        // Talent ID 5
        baseMultiplier += (double) talentTree.getTalentLevel(uuid, 7);

        return baseMultiplier / 100.0;
    }

    public boolean hasSellData(UUID uuid) {
        return sellDataMap.containsKey(uuid);
    }

    public void unloadSellData(UUID uuid) {
        if (!sellDataMap.containsKey(uuid)) return;
        sellDataMap.remove(uuid);
    }

    public double getDropsData(UUID uuid) {
        SellGUI.SellMultiplierData data = sellDataMap.get(uuid);
        return (data == null) ? 0.0 : data.drops();
    }

    public double getTaxData(UUID uuid) {
        SellGUI.SellMultiplierData data = sellDataMap.get(uuid);
        return (data == null) ? 0.0 : data.taxes();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            lastGuiOpen.clear();
        }
    }
}
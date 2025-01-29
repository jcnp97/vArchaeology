package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.logs.SalvageLog;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SalvageGUI implements Listener {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final ConfigManager configManager;
    private final Statistics statistics;
    private final SalvageLog salvageLog;
    private final NamespacedKey varchItemKey;

    public SalvageGUI(Main plugin, EffectsUtil effectsUtil, Statistics statistics, ConfigManager configManager, SalvageLog salvageLog) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.statistics = statistics;
        this.configManager = configManager;
        this.salvageLog = salvageLog;
        this.varchItemKey = new NamespacedKey(plugin, "varch_item");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openComponentsGUI(Player player) {
        ChestGui gui = new ChestGui(5, configManager.compMenu);
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = createStaticPaneComponents(player);

        gui.addPane(staticPane);
        gui.show(player);
    }

    private StaticPane createStaticPaneComponents(Player player) {
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);
        UUID playerUUID = player.getUniqueId();

        ItemStack salvageButton = buttonToSalvageGUI();
        GuiItem guiItem = new GuiItem(salvageButton, event -> openSalvageGUI(player));
        staticPane.addItem(guiItem, 4, 2);

        String[] componentName = {
                "Common",
                "Uncommon",
                "Rare",
                "Unique",
                "Special",
                "Mythical",
                "Exotic"
        };
        for (int x = 1; x <= 7; x++) {
            ItemStack compButton = createComponentButton(x, componentName[x - 1], playerUUID);
            staticPane.addItem(new GuiItem(compButton), x, 1);
        }
        return staticPane;
    }

    private ItemStack buttonToSalvageGUI() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§eInformation");
            meta.setCustomModelData(configManager.confirmModelData);
            meta.setLore(List.of(
                    "§7Components are typically used to",
                    "§7craft tools, augments, or restore",
                    "§7unidentified artefacts.",
                    "",
                    "§7To obtain components, you need to",
                    "§7disassemble or salvage archaeology",
                    "§7items through §b[/varch salvage]§7."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createComponentButton(int index, String componentName, UUID playerUUID) {
        ItemStack button = new ItemStack(Material.FLINT);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§e" + componentName + " Component");
            meta.setCustomModelData(index + 99999);
            meta.setLore(List.of(
                    "§7Amount: §a" + statistics.getStatistics(playerUUID, index + 1)
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    public void openSalvageGUI(Player player) {
        Map<Integer, Integer> initialValue = calculateInventoryValue(player);

        //ChestGui gui = new ChestGui(5, "§f\uE0F1\uE0F1\uE053\uE0FA");
        ChestGui gui = new ChestGui(5, configManager.salvageMenu);
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = createStaticPane(player, initialValue);
        OutlinePane itemPane = createItemPane(player);

        gui.addPane(staticPane);
        gui.addPane(itemPane);
        gui.show(player);
    }

    private StaticPane createStaticPane(Player player, Map<Integer, Integer> initialValue) {
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);

        for (int x = 1; x <= 3; x++) {
            ItemStack sellButton = createSalvageButton(initialValue);
            GuiItem guiItem = new GuiItem(sellButton, event -> processSalvageAction(player, initialValue));
            staticPane.addItem(guiItem, x, 4);
        }
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 4);
        }
        return staticPane;
    }

    private void processSalvageAction(Player player, Map<Integer, Integer> initialValue) {
        if (initialValue == null || initialValue.isEmpty()) {
            player.sendMessage("§cNo items to salvage!");
            player.closeInventory();
            return;
        }

        boolean hasNonZeroValue = initialValue.values().stream()
                .anyMatch(value -> value > 0);

        if (!hasNonZeroValue) {
            player.sendMessage("§cNo items to salvage!");
            player.closeInventory();
            return;
        }

        Map<Integer, Integer> currentValue = calculateInventoryValue(player);
        if (!initialValue.equals(currentValue)) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        try {
            UUID playerUUID = player.getUniqueId();
            takeItems(player);
            String[] componentName = {
                    "Common",
                    "Uncommon",
                    "Rare",
                    "Unique",
                    "Special",
                    "Mythical",
                    "Exotic"
            };
            for (int i = 1; i <= 7; i++) {
                if (currentValue.get(i) != 0) {
                    salvageLog.logTransaction(player.getName(), componentName[i - 1], currentValue.get(i));
                    statistics.addStatistics(playerUUID, i + 1, currentValue.get(i));
                    player.sendMessage("§aYou have obtained §a" + currentValue.get(i) + "x §e" + componentName[i - 1] + " Components.");
                }
            }
            effectsUtil.playSound(player, "minecraft:block.anvil.use", Sound.Source.PLAYER, 1.0f, 1.0f);
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred during the process. Please try again.");
            plugin.getLogger().severe("Error processing items for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
    }

    private ItemStack createSalvageButton(Map<Integer, Integer> totalValue) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§aSalvage Items §f\uE072");
            meta.setCustomModelData(configManager.confirmModelData);
            meta.setLore(List.of(
                    "",
                    "§7You will receive:",
                    "§a" + totalValue.get(1) + "x §eCommon Components",
                    "§a" + totalValue.get(2) + "x §eUncommon Components",
                    "§a" + totalValue.get(3) + "x §eRare Components",
                    "§a" + totalValue.get(4) + "x §eUnique Components",
                    "§a" + totalValue.get(5) + "x §eSpecial Components",
                    "§a" + totalValue.get(6) + "x §eMythical Components",
                    "§a" + totalValue.get(7) + "x §eExotic Components"
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
            meta.setCustomModelData(configManager.cancelModelData);
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

    private Map<Integer, Integer> calculateInventoryValue(Player player) {
        Map<Integer, Integer> valueMap = new HashMap<>();

        for (int i = 1; i <= 7; i++) {
            valueMap.put(i, 0);
        }
        Arrays.stream(player.getInventory().getContents())
                .filter(this::isSellable)
                .forEach(item -> {
                    Integer customId = getCustomId(item);
                    if (customId != null && item != null) {
                        valueMap.merge(customId, item.getAmount(), Integer::sum);
                    }
                });
        return valueMap;
    }

    private void takeItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isSellable(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}
package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SellGUIManager {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final NamespacedKey varchItemKey;
    private ChestGui currentGui;
    private List<GuiItem> sellButtons;

    public SellGUIManager(Main plugin, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.varchItemKey = new NamespacedKey(plugin, "varch_item");
        this.sellButtons = new ArrayList<>();
    }

    public void openSellGUI(Player player) {
        ChestGui gui = new ChestGui(5, "§f\uE0F1\uE0F1\uE053\uD833\uDEAF");
        this.currentGui = gui;
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = createStaticPane();
        OutlinePane itemPane = createItemPane(player);

        gui.addPane(staticPane);
        gui.addPane(itemPane);
        gui.show(player);
    }

    private StaticPane createStaticPane() {
        StaticPane staticPane = new StaticPane(0, 0, 9, 5);

        for (int x = 1; x <= 3; x++) {
            ItemStack sellButton = createSellButton(0);
            GuiItem guiItem = new GuiItem(sellButton, event -> {
                if (!(event.getWhoClicked() instanceof Player)) return;
                Player player = (Player) event.getWhoClicked();
                int totalValue = calculateAndSellItems(player);
                if (totalValue > 0) {
                    player.sendMessage("§aYou sold your items for §6" + totalValue + " coins!");
                    effectsUtil.playSound(player, "minecraft:cozyvanilla.sell_confirmed", Sound.Source.PLAYER, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§cNo sellable items found in your inventory!");
                }
                player.closeInventory();
            });
            sellButtons.add(guiItem);
            staticPane.addItem(guiItem, x, 4);
        }
        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 4);
        }
        return staticPane;
    }

    private ItemStack createSellButton(int initialValue) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        updateSellButtonLore(button, initialValue);
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
        List<ItemStack> itemsToSell = new ArrayList<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSellable(item)) {
                ItemStack itemCopy = item.clone();
                itemPane.addItem(new GuiItem(itemCopy, event -> {
                    itemsToSell.add(itemCopy);
                    updateAllSellButtonsLore(calculateTotalValue(itemsToSell));
                }));
            }
        }
        return itemPane;
    }

    private void updateAllSellButtonsLore(int totalValue) {
        for (GuiItem button : sellButtons) {
            updateSellButtonLore(button.getItem(), totalValue);
        }
        if (currentGui != null) {
            currentGui.update();
        }
    }

    private void updateSellButtonLore(ItemStack sellButton, int totalValue) {
        ItemMeta meta = sellButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aSell Items");
            meta.setLore(List.of(
                    "§7Current total: §6" + totalValue + " coins",
                    "§eClick to sell all sellable items!"
            ));
            sellButton.setItemMeta(meta);
        }
    }

    private boolean isSellable(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer customID = pdc.get(varchItemKey, PersistentDataType.INTEGER);

        return customID != null && customID >= 1 && customID <= 7;
    }

    private int getItemValue(ItemStack item) {
        if (!isSellable(item)) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int customID = pdc.get(varchItemKey, PersistentDataType.INTEGER);

        return switch (customID) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            case 4 -> 40;
            case 5 -> 50;
            case 6 -> 60;
            case 7 -> 70;
            default -> 0;
        };
    }

    private int calculateTotalValue(List<ItemStack> items) {
        return items.stream()
                .mapToInt(item -> getItemValue(item) * item.getAmount())
                .sum();
    }

    private int calculateAndSellItems(Player player) {
        int totalValue = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isSellable(item)) {
                totalValue += getItemValue(item) * item.getAmount();
                player.getInventory().setItem(i, null);
            }
        }
        return totalValue;
    }
}
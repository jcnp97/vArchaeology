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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class SellGUI implements Listener {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final NamespacedKey varchItemKey;
    private static final long GUI_COOLDOWN = 500;
    private final Map<UUID, Long> lastGuiOpen = new HashMap<>();

    public SellGUI(Main plugin, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
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
        int initialValue = calculateInventoryValue(player);

        ChestGui gui = new ChestGui(5, "§f\uE0F1\uE0F1\uE053\uD833\uDEAF");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = createStaticPane(player, initialValue);
        OutlinePane itemPane = createItemPane(player);

        gui.addPane(staticPane);
        gui.addPane(itemPane);
        gui.show(player);
    }

    private StaticPane createStaticPane(Player player, int initialValue) {
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

    private void processSellAction(Player player, int initialValue) {
        int currentValue = calculateInventoryValue(player);

        if (currentValue != initialValue) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        try {
            if (currentValue > 0) {
                sellItems(player, currentValue);
                player.sendMessage("§aYou sold your items for §6" + currentValue + " coins!");
                effectsUtil.playSound(player, "minecraft:cozyvanilla.sell_confirmed", Sound.Source.PLAYER, 1.0f, 1.0f);
                logTransaction(player, currentValue);
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

    private ItemStack createSellButton(int totalValue) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aSell Items");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§7Current total: §6" + totalValue + " coins",
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

    private int getItemValue(ItemStack item) {
        Integer customID = getCustomId(item);
        if (customID == null) return 0;

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

    private int calculateInventoryValue(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && isSellable(item))
                .mapToInt(item -> getItemValue(item) * item.getAmount())
                .sum();
    }

    private void sellItems(Player player, int totalValue) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isSellable(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
        // Add coins to player's balance here
    }

    private void logTransaction(Player player, int value) {
        String itemDetails = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && isSellable(item))
                .map(item -> String.format("%dx ID:%d", item.getAmount(), getCustomId(item)))
                .collect(Collectors.joining(", "));

        plugin.getLogger().info(String.format(
                "Transaction: Player=%s, Value=%d, Items=[%s]",
                player.getName(),
                value,
                itemDetails
        ));
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            lastGuiOpen.clear();
        }
    }
}
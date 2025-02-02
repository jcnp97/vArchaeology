package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.*;

public class CollectionsGUI {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final ConfigManager configManager;
    private final PlayerData playerData;
    private final SellGUI sellGUI;
    private final NamespacedKey COLLECTION_KEY;
    private final DecimalFormat decimalFormat;

    public CollectionsGUI(Main plugin,
                          EffectsUtil effectsUtil,
                          ConfigManager configManager,
                          PlayerData playerData,
                          SellGUI sellGUI) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.configManager = configManager;
        this.playerData = playerData;
        this.sellGUI = sellGUI;
        this.decimalFormat = new DecimalFormat("#,###");
        this.COLLECTION_KEY = new NamespacedKey(plugin, "varch_collection");
    }

    public void openCollectionGUI(Player player) {
        UUID uuid = player.getUniqueId();
        int archAptitude = playerData.getArchApt(uuid);
        double artefactsMultiplier = sellGUI.getArtefactsData(uuid);
        double totalPrice = Math.round((archAptitude + 25000) * artefactsMultiplier * 100.0) / 100.0;

        ChestGui gui = new ChestGui(3, configManager.collectionInteractTitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        ItemStack monolithButton = createMonolithButton();
        GuiItem monolith = new GuiItem(monolithButton, event -> openConfirmationGUI(player, 1, totalPrice));
        staticPane.addItem(monolith, 1, 1);

        ItemStack sellButton = createSellButton(totalPrice, archAptitude, artefactsMultiplier);
        GuiItem sell = new GuiItem(sellButton, event -> openConfirmationGUI(player, 2, totalPrice));
        staticPane.addItem(sell, 4, 1);

        for (int x = 3; x <= 5; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 2);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createMonolithButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§2Offer to Archaeology Monolith");
            meta.setCustomModelData(configManager.invisibleModelData);
            meta.setLore(List.of(
                    "§7You will receive §a+1 §7talent point",
                    "§7that is usable on §b[/varch talent]§7."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createSellButton(double totalPrice, int archAptitude, double mult) {
        ItemStack button = new ItemStack(Material.PAPER);
        String formatPrice = decimalFormat.format(totalPrice);
        String aptitude = decimalFormat.format(archAptitude);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§2Sell to Collectors");
            meta.setCustomModelData(configManager.invisibleModelData);
            meta.setLore(List.of(
                    "§7Total Price: §a$" + formatPrice,
                    "§7Aptitude (Bonus Base): §a$" + aptitude,
                    "§7Multiplier: §a" + mult + "X"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    public void openConfirmationGUI(Player player, int processType, double totalPrice) {

        ChestGui gui = new ChestGui(3, configManager.confirmGUITitle);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        StaticPane staticPane = new StaticPane(0, 0, 9, 3);

        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createConfirmButton();
            staticPane.addItem(new GuiItem(confirmButton, event -> processAction(player, processType, totalPrice)), x, 1);
        }

        for (int x = 5; x <= 7; x++) {
            ItemStack cancelButton = createCancelButton();
            staticPane.addItem(new GuiItem(cancelButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private void processAction(Player player, int processType, double totalValue) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isCollection(item)) {
            player.sendMessage("§cYour mainhand does not contain any collection artefacts!");
            return;
        }

        try {
            player.getInventory().removeItem(item);
            if (processType == 1) {
                playerData.incrementTalentPoints(uuid);
                effectsUtil.spawnFireworks(uuid, 3, 3);
                effectsUtil.sendPlayerMessage(uuid,"<#00FFA2>You have <yellow>" +
                        playerData.getTalentPoints(uuid) + " <#00FFA2>talent points that you can spend on <aqua>[/varch talent]<#00FFA2>.");
            } else if (processType == 2) {
                sellGUI.addEconomy(player, totalValue);
                effectsUtil.spawnFireworks(uuid, 3, 3);
                String formatPrice = decimalFormat.format(totalValue);
                effectsUtil.sendTitleMessage(uuid, "", "<#7CFEA7>You have received <gold>$" +
                        formatPrice + "<#7CFEA7>!");
            } else {
                player.sendMessage("§cThere was an error processing the lamp. Please contact the administrator.");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing lamps. Please try again.");
            plugin.getLogger().severe("Error processing xp lamp for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
    }

    private boolean isCollection(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(COLLECTION_KEY, PersistentDataType.INTEGER);
    }

    private ItemStack createConfirmButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aConfirm transaction");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cClose");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createCancelButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cCancel");
            meta.setCustomModelData(configManager.invisibleModelData);
            button.setItemMeta(meta);
        }
        return button;
    }
}
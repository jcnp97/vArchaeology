package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.exp.EXPManager;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class ConfirmationGUI {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final EXPManager expManager;
    private final NamespacedKey LAMP_KEY;
    private final NamespacedKey STAR_KEY;

    public ConfirmationGUI(Main plugin, EffectsUtil effectsUtil, EXPManager expManager) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.expManager = expManager;
        this.LAMP_KEY = new NamespacedKey(plugin, "varch_lamp");
        this.STAR_KEY = new NamespacedKey(plugin, "varch_star");
    }

    public void openConfirmationLamp(Player player, int lampType) {
        UUID uuid = player.getUniqueId();
        int initialAmount = player.getInventory().getItemInMainHand().getAmount();
        double initialXP = expManager.getLampXP(uuid, lampType, initialAmount);

        ChestGui gui = new ChestGui(2, "§fTest");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = new StaticPane(0, 0, 9, 2);

        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createConfirmButtonXP(initialXP, initialAmount);
            GuiItem guiItem = new GuiItem(confirmButton, event -> processXPAction(player, initialXP, lampType));
            staticPane.addItem(guiItem, x, 1);
        }

        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createConfirmButtonXP(double exp, int amount) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eUse §a" + amount + "x §eXP Lamps?");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§7You will receive §6" + formattedXP + " XP§7."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processXPAction(Player player, double initialXP, int lampType) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();
        int finalAmount = player.getInventory().getItemInMainHand().getAmount();
        double finalXP = expManager.getLampXP(uuid, lampType, finalAmount);

        if (initialXP != finalXP) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        try {
            if (finalXP > 0) {
                player.getInventory().removeItem(item);
                expManager.addLampXP(uuid, finalXP);
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

    public void openConfirmationStar(Player player, int starType) {
        UUID uuid = player.getUniqueId();
        int initialAmount = player.getInventory().getItemInMainHand().getAmount();
        int initialBXP = expManager.getStarXP(uuid, starType, initialAmount);

        ChestGui gui = new ChestGui(2, "§fTest");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = new StaticPane(0, 0, 9, 2);

        for (int x = 1; x <= 3; x++) {
            ItemStack confirmButton = createConfirmButtonBXP(initialBXP, initialAmount);
            GuiItem guiItem = new GuiItem(confirmButton, event -> processBXPAction(player, initialBXP, starType));
            staticPane.addItem(guiItem, x, 1);
        }

        for (int x = 5; x <= 7; x++) {
            ItemStack closeButton = createCloseButton();
            staticPane.addItem(new GuiItem(closeButton, event -> event.getWhoClicked().closeInventory()), x, 1);
        }

        gui.addPane(staticPane);
        gui.show(player);
    }

    private ItemStack createConfirmButtonBXP(int exp, int amount) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedXP = decimalFormat.format(exp);
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eUse §a" + amount + "x §eXP Stars?");
            meta.setCustomModelData(10367);
            meta.setLore(List.of(
                    "§7You will receive §6" + formattedXP + " Bonus XP§7."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processBXPAction(Player player, int initialBXP, int lampType) {
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();
        int finalBXP = expManager.getStarXP(uuid, lampType, player.getInventory().getItemInMainHand().getAmount());

        if (initialBXP != finalBXP) {
            player.sendMessage("§cError: Inventory has changed. Please reopen the GUI.");
            player.closeInventory();
            return;
        }

        try {
            if (finalBXP > 0) {
                player.getInventory().removeItem(item);
                expManager.addStarXP(uuid, finalBXP);
            } else {
                player.sendMessage("§cThere was an error processing the star. Please contact the administrator.");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing stars. Please try again.");
            plugin.getLogger().severe("Error processing xp star for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
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
}
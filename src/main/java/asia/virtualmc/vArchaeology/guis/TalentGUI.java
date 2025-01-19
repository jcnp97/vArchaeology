package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TalentGUI {

    private final Main plugin;
    private final EffectsUtil effectsUtil;

    public TalentGUI(Main plugin, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
    }

    public void openTalentInfo(Player player) {
        ChestGui gui = new ChestGui(3, "Talent Information");
        StaticPane pane = new StaticPane(0, 0, 9, 3);

        for (int i = 0; i < 14; i++) {
            final int index = i;  // Create final copy for lambda
            ItemStack item = createItem(
                    Material.BOOK,
                    1,
                    "Talent " + (i + 1),
                    Arrays.asList("Info about Talent " + (i + 1))
            );

            pane.addItem(new GuiItem(item, event -> {
                event.setCancelled(true);
                if (index == 12) {
                    player.closeInventory();
                } else if (index == 13) {
                    openTalentUp(player);
                }
            }), i % 9, i / 9);
        }

        ItemStack levelItem = createItem(
                Material.EXPERIENCE_BOTTLE,
                1,
                "Your Level",
                Collections.singletonList("Current Level: " + player.getLevel())
        );
        pane.addItem(new GuiItem(levelItem, event -> event.setCancelled(true)), 0, 0);

        gui.addPane(pane);
        gui.show(player);
    }

    public void openTalentUp(Player player) {
        ChestGui gui = new ChestGui(3, "Talent Upgrade");
        StaticPane pane = new StaticPane(0, 0, 9, 3);

        for (int i = 0; i < 11; i++) {
            ItemStack item = createItem(
                    Material.ARROW,
                    1,
                    "Increase Talent " + (i + 1),
                    Collections.singletonList("Click to modify amount")
            );

            pane.addItem(new GuiItem(item, event -> {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null) {
                    clickedItem.setAmount(Math.min(clickedItem.getAmount() + 1, 64));
                }
            }), i % 9, i / 9);
        }

        ItemStack confirmItem = createItem(
                Material.EMERALD_BLOCK,
                1,
                "Confirm",
                Collections.singletonList("Confirm your selection")
        );
        pane.addItem(new GuiItem(confirmItem, event -> {
            event.setCancelled(true);
            openTalentInfo(player);
        }), 7, 1);

        ItemStack cancelItem = createItem(
                Material.REDSTONE_BLOCK,
                1,
                "Cancel",
                Collections.singletonList("Cancel and return")
        );
        pane.addItem(new GuiItem(cancelItem, event -> {
            event.setCancelled(true);
            openTalentInfo(player);
        }), 8, 1);

        gui.addPane(pane);
        gui.show(player);
    }

    private ItemStack createItem(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
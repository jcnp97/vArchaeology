package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.guis.ConfirmationGUI;
import asia.virtualmc.vArchaeology.items.MiscItems;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.UUID;

public class PlayerInteractListener implements Listener {
    private final Main plugin;
    private final MiscItems miscItems;
    private final ConfirmationGUI confirmationGUI;
    private final NamespacedKey LAMP_KEY;
    private final NamespacedKey STAR_KEY;

    public PlayerInteractListener(
            Main plugin,
            MiscItems miscItems,
            ConfirmationGUI confirmationGUI) {
        this.plugin = plugin;
        this.miscItems = miscItems;
        this.confirmationGUI = confirmationGUI;
        this.LAMP_KEY = new NamespacedKey(plugin, "varch_lamp");
        this.STAR_KEY = new NamespacedKey(plugin, "varch_star");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();

        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(LAMP_KEY)) {
            event.setCancelled(true);
            confirmationGUI.openConfirmationLamp(player, miscItems.getLampID(item));
        } else if (pdc.has(STAR_KEY)) {
            event.setCancelled(true);
            confirmationGUI.openConfirmationStar(player, miscItems.getStarID(item));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();

        if (!item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(LAMP_KEY)) {
            event.setCancelled(true);
            confirmationGUI.openConfirmationLamp(player, miscItems.getLampID(item));
        } else if (pdc.has(STAR_KEY)) {
            event.setCancelled(true);
            confirmationGUI.openConfirmationStar(player, miscItems.getStarID(item));
        }
    }
}
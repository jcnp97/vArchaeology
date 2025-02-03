package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.guis.CollectionsGUI;
import asia.virtualmc.vArchaeology.guis.LampStarGUI;
import asia.virtualmc.vArchaeology.items.MiscItems;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ItemInteractListener implements Listener {
    private final Main plugin;
    private CollectionsGUI collectionsGUI;
    private final MiscItems miscItems;
    private final LampStarGUI lampStarGUI;
    private final NamespacedKey LAMP_KEY;
    private final NamespacedKey STAR_KEY;
    private final NamespacedKey COLLECTION_KEY;

    public ItemInteractListener(Main plugin,
                                CollectionsGUI collectionsGUI,
                                MiscItems miscItems,
                                LampStarGUI lampStarGUI) {
        this.plugin = plugin;
        this.collectionsGUI = collectionsGUI;
        this.miscItems = miscItems;
        this.lampStarGUI = lampStarGUI;
        this.COLLECTION_KEY = new NamespacedKey(plugin, "varch_collection");
        this.LAMP_KEY = new NamespacedKey(plugin, "varch_lamp");
        this.STAR_KEY = new NamespacedKey(plugin, "varch_star");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (!pdc.has(COLLECTION_KEY, PersistentDataType.INTEGER)
                && !pdc.has(LAMP_KEY, PersistentDataType.INTEGER)
                && !pdc.has(STAR_KEY, PersistentDataType.INTEGER)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (pdc.has(COLLECTION_KEY, PersistentDataType.INTEGER)) {
            collectionsGUI.openCollectionGUI(player);
        } else if (pdc.has(LAMP_KEY, PersistentDataType.INTEGER)) {
            lampStarGUI.openConfirmationLamp(player, miscItems.getLampID(item));
        } else if (pdc.has(STAR_KEY, PersistentDataType.INTEGER)) {
            lampStarGUI.openConfirmationStar(player, miscItems.getStarID(item));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(LAMP_KEY, PersistentDataType.INTEGER)
                && !pdc.has(STAR_KEY, PersistentDataType.INTEGER)) {
            return;
        }

        event.setCancelled(true);

        if (pdc.has(LAMP_KEY, PersistentDataType.INTEGER)) {
            lampStarGUI.openConfirmationLamp(player, miscItems.getLampID(item));
        } else if (pdc.has(STAR_KEY, PersistentDataType.INTEGER)) {
            lampStarGUI.openConfirmationStar(player, miscItems.getStarID(item));
        }
    }
}

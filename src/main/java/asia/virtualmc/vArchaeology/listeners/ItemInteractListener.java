package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

public class ItemInteractListener implements Listener {
    private final Main plugin;
    private final NamespacedKey COLLECTION_KEY;

    public ItemInteractListener(Main plugin) {
        this.plugin = plugin;
        this.COLLECTION_KEY = new NamespacedKey(plugin, "varch_collection");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isCollection(item)) {
            Player player = event.getPlayer();
            player.sendMessage("§eYou interacted with a collection item!");
        }
    }

    private boolean isCollection(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(COLLECTION_KEY, PersistentDataType.INTEGER);
    }
}

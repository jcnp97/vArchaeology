package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MiscListener implements Listener {
    private final Main plugin;

    public MiscListener(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework) {
            Firework firework = (Firework) event.getDamager();
            if (firework.hasMetadata("nodamage")) {
                event.setCancelled(true);
            }
        }
    }
}
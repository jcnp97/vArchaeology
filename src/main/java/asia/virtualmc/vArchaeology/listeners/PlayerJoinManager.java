package asia.virtualmc.vArchaeology.listeners;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerJoinManager implements Listener {
    private final Main plugin;
    private final DatabaseManager databaseManager;

    public PlayerJoinManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("archaeology.use") || !event.getPlayer().isOp()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        databaseManager.getPlayerDataMap().computeIfAbsent(uuid, id -> new PlayerData(uuid, playerName));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        databaseManager.updatePlayerData(uuid);
        databaseManager.getPlayerDataMap().remove(uuid);
    }
}

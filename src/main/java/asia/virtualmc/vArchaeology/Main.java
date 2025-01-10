package asia.virtualmc.vArchaeology;

import asia.virtualmc.vArchaeology.storage.DatabaseManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;

import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public final class Main extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private PlayerJoinManager playerJoinManager;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        this.databaseManager = new DatabaseManager(this);
        this.playerDataManager = new PlayerDataManager(this, databaseManager);
        this.playerJoinManager = new PlayerJoinManager(this, databaseManager, playerDataManager);
        this.adventure = BukkitAudiences.create(this);

        getServer().getPluginManager().registerEvents(playerJoinManager, this);
        adventure.console().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#0000ff>[vArchaeology] has been enabled!</gradient>"));
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.updateAllData(); // Save all data before shutdown
        }
        getLogger().info("[vArchaeology] Closing database connections..");
        if (databaseManager != null) {
            databaseManager.closeConnection();
        } else {
            getLogger().severe("[vArchaeology] Failed to close database connections.");
        }
    }
}

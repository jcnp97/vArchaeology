// BossBarUtil.class
package asia.virtualmc.vArchaeology.utilities;

import asia.virtualmc.vArchaeology.Main;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarUtil {

    private final Main plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public BossBarUtil(Main plugin) {
        this.plugin = plugin;
        startBossBarUpdater();
    }

    /**
     * This method is called by PlayerDataManager to update the boss bar for the player.
     *
     * @param uuid       The player's UUID.
     * @param exp        The experience gained in this update.
     * @param level      The player's current level.
     * @param archExp    The player's current experience.
     * @param nextLevel  The experience needed to next level.
     */
    public void bossBarUpdate(@NotNull UUID uuid, int exp, int level, int archExp, int nextLevel) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        float progress = ((float) archExp / nextLevel);

        BossBar bossBar = bossBars.computeIfAbsent(uuid, k -> createBossBar(player));
        bossBar.name(Component.text()
                .append(Component.text("Archaeology Lv. " + level, NamedTextColor.GOLD))
                .append(Component.text(" (+" + exp + " EXP)", NamedTextColor.GREEN))
                .build());
        bossBar.progress(progress);
        player.showBossBar(bossBar);
    }

    /**
     * Creates a new boss bar with default settings.
     */
    private BossBar createBossBar(Player player) {
        return BossBar.bossBar(Component.text(""), 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    /**
     * Starts a global task that updates the boss bars and hides them if no updates are made for 5 seconds.
     */
    private void startBossBarUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : bossBars.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    BossBar bossBar = bossBars.get(uuid);

                    // Hide the boss bar if no updates have been made in the last 5 seconds
                    if (bossBar.progress() == 1.0f) {
                        player.hideBossBar(bossBar);
                    }
                }
            }
        }, 0L, 100L); // Runs every 5 seconds
    }

    /**
     * Clears the boss bar for a player.
     */
    public void clearBossBar(UUID uuid) {
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        }
    }
}

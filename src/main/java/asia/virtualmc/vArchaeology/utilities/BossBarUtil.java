package asia.virtualmc.vArchaeology.utilities;

import asia.virtualmc.vArchaeology.Main;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarUtil {

    private final Main plugin;
    private final Map<UUID, BossBarData> expUpdates;
    private final Map<UUID, BossBar> activeBossBars;

    public BossBarUtil(Main plugin) {
        this.plugin = plugin;
        this.expUpdates = new ConcurrentHashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        startBossBarTask();
    }

    /**
     * @param uuid       The player's UUID.
     * @param newExp        The experience gained in this update.
     * @param currentExp      The player's current level.
     * @param nextLevelExp    The player's current experience.
     * @param currentLevel  The experience needed to next level.
     */
    public void bossBarUpdate(@NotNull UUID uuid, int newExp, int currentExp, int nextLevelExp, int currentLevel) {
        BossBar bossBar = activeBossBars.computeIfAbsent(uuid, k -> BossBar.bossBar(
                Component.text(""),
                0.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        ));

        expUpdates.merge(uuid, new BossBarData(newExp, currentExp, nextLevelExp, currentLevel), (existing, update) -> {
            existing.addExp(update.getNewExp());
            existing.updateCurrentExp(update.getCurrentExp());
            existing.updateNextLevelExp(update.getNextLevelExp());
            existing.updateCurrentLevel(update.getCurrentLevel());
            return existing;
        });
    }

    private void startBossBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Run the main logic synchronously to avoid threading issues with Bukkit API
                Bukkit.getScheduler().runTask(plugin, () -> {
                    expUpdates.forEach((uuid, data) -> {
                        Player player = Bukkit.getPlayer(uuid);
                        BossBar bossBar = activeBossBars.get(uuid);

                        if (player != null && player.isOnline() && bossBar != null) {
                            int currentExp = data.getCurrentExp();
                            int nextLevelExp = data.getNextLevelExp();
                            int totalExp = data.getNewExp();

                            float progress = (float) currentExp / nextLevelExp;

                            bossBar.name(Component.text()
                                    .append(Component.text("Archaeology Lv. " + data.getCurrentLevel(), NamedTextColor.WHITE))
                                    .append(Component.text(" (+" + totalExp + " EXP)", NamedTextColor.GREEN))
                                    .build());
                            bossBar.progress(Math.min(progress, 1.0f));

                            player.showBossBar(bossBar);

                            // Schedule hiding the boss bar
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.hideBossBar(bossBar);
                                    activeBossBars.remove(uuid);
                                }
                            }.runTaskLater(plugin, 60L); // 3 seconds
                        }
                    });
                    expUpdates.clear();
                });
            }
        }.runTaskTimer(plugin, 200L, 200L); // Changed to runTaskTimer since we handle async/sync internally
    }

    private static class BossBarData {
        private int newExp;
        private int currentExp;
        private int nextLevelExp;
        private int currentLevel;

        public BossBarData(int newExp, int currentExp, int nextLevelExp, int currentLevel) {
            this.newExp = newExp;
            this.currentExp = currentExp;
            this.nextLevelExp = nextLevelExp;
            this.currentLevel = currentLevel;
        }

        public int getNewExp() {
            return newExp;
        }

        public int getCurrentExp() {
            return currentExp;
        }

        public int getNextLevelExp() {
            return nextLevelExp;
        }

        public int getCurrentLevel() {
            return currentLevel;
        }

        public void addExp(int exp) {
            this.newExp += exp;
        }

        public void updateCurrentExp(int currentExp) {
            this.currentExp = currentExp;
        }

        public void updateNextLevelExp(int nextLevelExp) {
            this.nextLevelExp = nextLevelExp;
        }

        public void updateCurrentLevel(int currentLevel) {
            this.currentLevel = currentLevel;
        }
    }
}
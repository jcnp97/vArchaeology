package asia.virtualmc.vArchaeology.guis;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.storage.CollectionLog;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankGUI {
    private final Main plugin;
    private final EffectsUtil effectsUtil;
    private final PlayerData playerData;
    private final Statistics statistics;
    private final ConfigManager configManager;
    private final CollectionLog collectionLog;
    private final ConcurrentHashMap<UUID, Integer> rankPointsMap;

    public RankGUI(Main plugin,
                   EffectsUtil effectsUtil,
                   PlayerData playerData,
                   Statistics statistics,
                   ConfigManager configManager,
                   CollectionLog collectionLog) {
        this.plugin = plugin;
        this.effectsUtil = effectsUtil;
        this.playerData = playerData;
        this.statistics = statistics;
        this.configManager = configManager;
        this.collectionLog = collectionLog;
        this.rankPointsMap = new ConcurrentHashMap<>();
    }

    public void openRankGUI(Player player) {
        UUID uuid = player.getUniqueId();

        if (!rankPointsMap.containsKey(uuid)) loadData(uuid);

        ChestGui gui = new ChestGui(4, "§f\uE0F1\uE0F1\uE053\uE0FE");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane staticPane = new StaticPane(0, 0, 9, 4);

        Map<Integer, GuiItem> statButtonItems = new HashMap<>();
        int rankAchieved = statistics.getStatistics(uuid, 1);
        int currentPoints = rankPointsMap.getOrDefault(uuid, 0);
        int nextRank = configManager.rankTable.get(rankAchieved + 1).pointsRequired();
        String nextRankName = configManager.rankTable.get(rankAchieved + 1).rankName();
        String currentRankName = configManager.rankTable.get(rankAchieved).rankName();

        for (int x = 1; x <= 7; x++) {
            ItemStack statButton = createStatButton(rankAchieved, currentPoints, nextRank);
            GuiItem guiItem = new GuiItem(statButton);
            staticPane.addItem(guiItem, x, 1);
            statButtonItems.put(x, guiItem);
        }

        if (currentPoints >= nextRank) {
            for (int x = 3; x <= 5; x++) {
                ItemStack confirmButton = createConfirmButton();
                staticPane.addItem(new GuiItem(confirmButton, event -> processRankUp(player, currentPoints, nextRank, currentRankName, nextRankName)), x, 2);
            }
        } else {
            for (int x = 3; x <= 5; x++) {
                ItemStack confirmButton = createNoAccess();
                staticPane.addItem(new GuiItem(confirmButton), x, 2);
            }
        }

        ItemStack infoButton = createInfoButton(uuid);
        staticPane.addItem(new GuiItem(infoButton), 8, 0);

        gui.addPane(staticPane);
        gui.show(player);

        // modifications
        double progress = Math.min(100, ((double) currentPoints/nextRank) * 100);
        int progressChunk = (int) progress / 15;
        switch (progressChunk) {
            case 0 -> modifyStatButton0(statButtonItems, progress);
            case 1 -> modifyStatButton1(statButtonItems, progress - 15);
            case 2 -> modifyStatButton2(statButtonItems, progress - 30);
            case 3 -> modifyStatButton3(statButtonItems, progress - 45);
            case 4 -> modifyStatButton4(statButtonItems, progress - 60);
            case 5 -> modifyStatButton5(statButtonItems, progress - 75);
            case 6 -> modifyStatButton6(statButtonItems, progress - 90);
        }
        gui.update();
    }

    private ItemStack createStatButton(int rankAchieved, int currentPoints, int nextRank) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        double progress = Math.min(100, ((double) currentPoints / nextRank) * 100);
        String formattedProgress = String.format("%.2f", progress);
        if (meta != null) {
            meta.setDisplayName("§2" + configManager.rankTable.get(rankAchieved).rankName() +
                    " §7→ §4" + configManager.rankTable.get(rankAchieved + 1).rankName()
            );
            meta.setLore(List.of(
                    "§8§m§l                                           ",
                    "§7Current Pts: §e" + String.format("%,d", currentPoints),
                    "§7Next Rank: §e" + String.format("%,d", nextRank),
                    "§7Remainder: §e" + String.format("%,d", Math.max(0, nextRank - currentPoints)) + " §7(§a" + formattedProgress + "%§7)",
                    "§8§m§l                                           ",
                    "§7You gain §a+" + String.format("%.1f", (double) (rankAchieved)) + "% §7XP per block-break.",
                    "§7You gain §a+" + String.format("%.1f", (double) (rankAchieved) * 0.50) + "% §7XP per material drop.",
                    "§7You gain §a+" + String.format("%.1f", (double) (rankAchieved) * 0.25) + "% §7XP per artefact restore.",
                    "§7You gain §a+" + String.format("%.1f", (double) (rankAchieved) * 1.2) + "% §7tax reduction.",
                    "§8§m§l                                           ",
                    "§8Note: §7All bonuses are capped at Guildmaster I."
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createConfirmButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aConfirm rank-up");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createNoAccess() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNot enough points!");
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    private void processRankUp(Player player, int currentPoints, int nextRank, String currentRankName, String nextRankName) {
        UUID uuid = player.getUniqueId();

        try {
            if (currentPoints >= nextRank) {
                effectsUtil.sendTitleMessage(uuid, "<#00FFA2>Ranking Up", "<yellow>" + currentRankName + " <gray>→ <yellow>" + nextRankName);
                statistics.incrementStatistics(uuid, 1);
                effectsUtil.playSoundUUID(uuid, "minecraft:cozyvanilla.rankup_sounds", Sound.Source.PLAYER, 1.0f, 1.0f);
                effectsUtil.spawnFireworks(uuid, 5, 5);
            } else {
                player.sendMessage("§cThere was an error processing your rank-up. Please contact the administrator.");
            }
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while processing ranking-up. Please try again.");
            plugin.getLogger().severe("Error processing ranking-up for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        player.closeInventory();
    }

    private ItemStack createInfoButton(UUID uuid) {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lInformation");
            meta.setLore(List.of(
                    "§7Your points is calculated using the",
                    "§7following statistics:",
                    "",
                    "§7• §aArchaeology Level: §e" + playerData.getArchLevel(uuid),
                    "§7• §aBlocks Mined: §e" + statistics.getStatistics(uuid, 16),
                    "§7• §aMaterials Found: §e" + collectionLog.getDropsObtained(uuid),
                    "§7• §aExotic Found: §e" + statistics.getStatistics(uuid, 15),
                    "§7• §aArtefacts Found: §e" + statistics.getStatistics(uuid, 17),
                    "§7• §aArtefacts Restored: §e" + statistics.getStatistics(uuid, 18),
                    "§7• §aMoney Earned: §e$" + statistics.getStatistics(uuid, 20),
                    "§7• §aTaxes Paid: §e$" + statistics.getStatistics(uuid, 21),
                    "§7• §aAptitude: §e" + playerData.getArchApt(uuid)
            ));
            meta.setCustomModelData(10367);
            button.setItemMeta(meta);
        }
        return button;
    }

    public void loadData(UUID uuid) {
        if (rankPointsMap.containsKey(uuid)) return;

        double totalPoints = 0.0;
        // Archaeology Level
        totalPoints += playerData.getArchLevel(uuid) * 8400;
        // Blocks Mined
        totalPoints += statistics.getStatistics(uuid, 16) * 0.2;
        // Drops/Materials Obtained
        totalPoints += collectionLog.getDropsObtained(uuid) * 2;
        // Exotic Found
        totalPoints += collectionLog.getCollections(uuid, 7) * 300;
        // Artefacts Found
        totalPoints += statistics.getStatistics(uuid, 17) * 300;
        // Artefacts Restored
        totalPoints += statistics.getStatistics(uuid, 18) * 300;
        // Money Earned
        totalPoints += statistics.getStatistics(uuid, 20) * 0.01;
        // Taxes Paid
        totalPoints += statistics.getStatistics(uuid, 21) * 0.01;
        // Aptitude
        totalPoints += playerData.getArchApt(uuid);
        totalPoints = Math.round(totalPoints * 100.0) / 100.0;

        rankPointsMap.put(uuid, (int) totalPoints);
    }

    public void unloadData(UUID uuid) {
        if (!rankPointsMap.containsKey(uuid)) return;
        rankPointsMap.remove(uuid);
    }

    public void setRankPoints(UUID uuid, int points) {
        rankPointsMap.remove(uuid);
        rankPointsMap.put(uuid, points);
    }

    // Modifications
    private void modifyStatButton0(Map<Integer, GuiItem> buttons, double progress) {
        int progressChunk = (int) progress / 3;
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100000 + progressChunk));

        for (int i = 2; i < 7; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100005));
        }
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton1(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));

        int progressChunk = (int) progress / 3;
        buttons.get(2).setItem(setCustomModelData(buttons.get(2).getItem(), 100005 + progressChunk));

        for (int i = 3; i < 7; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100005));
        }
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton2(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));
        buttons.get(2).setItem(setCustomModelData(buttons.get(2).getItem(), 100009));

        int progressChunk = (int) progress / 3;
        buttons.get(3).setItem(setCustomModelData(buttons.get(3).getItem(), 100005 + progressChunk));

        for (int i = 4; i < 7; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100005));
        }
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton3(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));

        for (int i = 2; i < 4; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100009));
        }

        int progressChunk = (int) progress / 3;
        buttons.get(4).setItem(setCustomModelData(buttons.get(4).getItem(), 100005 + progressChunk));

        for (int i = 5; i < 7; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100005));
        }
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton4(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));

        for (int i = 2; i < 5; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100009));
        }

        int progressChunk = (int) progress / 3;
        buttons.get(5).setItem(setCustomModelData(buttons.get(5).getItem(), 100005 + progressChunk));
        buttons.get(6).setItem(setCustomModelData(buttons.get(6).getItem(), 100005));
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton5(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));

        for (int i = 2; i < 6; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100009));
        }

        int progressChunk = (int) progress / 3;
        buttons.get(6).setItem(setCustomModelData(buttons.get(6).getItem(), 100005 + progressChunk));
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010));
    }

    private void modifyStatButton6(Map<Integer, GuiItem> buttons, double progress) {
        buttons.get(1).setItem(setCustomModelData(buttons.get(1).getItem(), 100004));

        for (int i = 2; i < 7; i++) {
            buttons.get(i).setItem(setCustomModelData(buttons.get(i).getItem(), 100009));
        }

        int progressChunk = (int) progress / 2;
        buttons.get(7).setItem(setCustomModelData(buttons.get(7).getItem(), 100010 + progressChunk));
    }

    private ItemStack setCustomModelData(ItemStack item, int modelData) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
        }
        return item;
    }
}
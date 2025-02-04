package asia.virtualmc.vArchaeology.placeholders;

import asia.virtualmc.vArchaeology.Main;

import asia.virtualmc.vArchaeology.configs.ConfigManager;
import asia.virtualmc.vArchaeology.guis.RankGUI;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.Statistics;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlaceholderPlayerData extends PlaceholderExpansion {
    private final Main plugin;
    private final PlayerData playerData;
    private final Statistics statistics;
    private final ConfigManager configManager;
    private final RankGUI rankGUI;

    public PlaceholderPlayerData(Main plugin,
                                 PlayerData playerData,
                                 Statistics statistics,
                                 ConfigManager configManager,
                                 RankGUI rankGUI) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.statistics = statistics;
        this.configManager = configManager;
        this.rankGUI = rankGUI;
        this.register();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "varch";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null || !player.isOnline()) return "0";

        Player p = (Player) player;
        UUID uuid = p.getUniqueId();

        return switch (identifier) {
            case "level" -> String.valueOf(playerData.getArchLevel(uuid));
            case "exp" -> String.valueOf(playerData.getArchExp(uuid));
            case "player_name" -> String.valueOf(playerData.getPlayerName(uuid));
            case "luck" -> String.valueOf(playerData.getArchLuck(uuid));
            case "rank_points" -> String.valueOf(rankGUI.getRankPoints(uuid));
            case "rank_name" -> configManager.rankTable.get(statistics.getStatistics(uuid, 1)).rankName();
            default -> null;
        };

    }
}
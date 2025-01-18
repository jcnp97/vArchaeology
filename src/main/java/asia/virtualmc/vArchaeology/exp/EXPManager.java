package asia.virtualmc.vArchaeology.exp;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.PlayerDataManager;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class EXPManager {
    private final Main plugin;
    private final StatsManager statsManager;
    private final PlayerDataManager playerDataManager;
    private final TalentTreeManager talentTreeManager;

    public EXPManager(@NotNull Main plugin, @NotNull StatsManager statsManager, @NotNull PlayerDataManager playerDataManager, @NotNull TalentTreeManager talentTreeManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.playerDataManager = playerDataManager;
        this.talentTreeManager = talentTreeManager;
    }

    public double getTotalBlockBreakEXP(UUID playerUUID, float blockEXP) {
        int traitBonus = playerDataManager.getWisdomTrait(playerUUID) * 2;
        int talentBonus = (talentTreeManager.getTalentLevel(playerUUID, 1) * 25) + talentTreeManager.getTalentLevel(playerUUID, 9);
        int rankBonus = statsManager.getStatistics(playerUUID, 12) * 2;
        double totalXP = (((double) (traitBonus + talentBonus + rankBonus)/100) + playerDataManager.getArchXPMul(playerUUID)) * blockEXP;
        double bonusXP = 0.0;

        if (playerDataManager.getArchBonusXP(playerUUID) > 0) {
            double currentBonus = playerDataManager.getArchBonusXP(playerUUID);
            if (currentBonus >= totalXP) {
                bonusXP = totalXP;
                playerDataManager.reduceBonusXP(playerUUID, (int) totalXP);
            } else {
                bonusXP = currentBonus;
                playerDataManager.resetBonusXP(playerUUID);
            }
        }
        return totalXP + bonusXP;
    }
}

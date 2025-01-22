package asia.virtualmc.vArchaeology.exp;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.storage.PlayerData;

import asia.virtualmc.vArchaeology.storage.TalentTree;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class EXPManager {
    private final Main plugin;
    private final Statistics statistics;
    private final PlayerData playerData;
    private final TalentTree talentTree;

    public EXPManager(@NotNull Main plugin, @NotNull Statistics statistics, @NotNull PlayerData playerData, @NotNull TalentTree talentTree) {
        this.plugin = plugin;
        this.statistics = statistics;
        this.playerData = playerData;
        this.talentTree = talentTree;
    }

    public double getTotalBlockBreakEXP(UUID playerUUID, float blockEXP) {
        int traitBonus = playerData.getWisdomTrait(playerUUID);
        int talentBonus1 = talentTree.getTalentLevel(playerUUID, 1);
        int talentBonus2 = talentTree.getTalentLevel(playerUUID, 9);
        int rankBonus = statistics.getStatistics(playerUUID, 12);
        double archXPMul = playerData.getArchXPMul(playerUUID);

        double baseMultiplier = ((traitBonus * 2 +
                talentBonus1 * 25 +
                talentBonus2 +
                rankBonus * 2) / 100.0) + archXPMul;

        double totalXP = baseMultiplier * blockEXP;
        int currentBonus = playerData.getArchBonusXP(playerUUID);

        if (currentBonus <= 0) {
            return totalXP;
        }
        double bonusXP = Math.min(currentBonus, totalXP);

        if (bonusXP >= totalXP) {
            playerData.reduceBonusXP(playerUUID, (int) totalXP);
        } else {
            playerData.resetBonusXP(playerUUID);
        }
        return totalXP + bonusXP;
    }
}
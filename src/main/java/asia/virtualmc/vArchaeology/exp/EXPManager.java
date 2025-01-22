package asia.virtualmc.vArchaeology.exp;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.StatsManager;
import asia.virtualmc.vArchaeology.storage.PlayerData;
import asia.virtualmc.vArchaeology.storage.TalentTreeManager;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class EXPManager {
    private final Main plugin;
    private final StatsManager statsManager;
    private final PlayerData playerData;
    private final TalentTreeManager talentTreeManager;

    public EXPManager(@NotNull Main plugin, @NotNull StatsManager statsManager, @NotNull PlayerData playerData, @NotNull TalentTreeManager talentTreeManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.playerData = playerData;
        this.talentTreeManager = talentTreeManager;
    }

    public double getTotalBlockBreakEXP(UUID playerUUID, float blockEXP) {
        int traitBonus = playerData.getWisdomTrait(playerUUID);
        int talentBonus1 = talentTreeManager.getTalentLevel(playerUUID, 1);
        int talentBonus2 = talentTreeManager.getTalentLevel(playerUUID, 9);
        int rankBonus = statsManager.getStatistics(playerUUID, 12);
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
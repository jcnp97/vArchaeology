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
        int traitBonus = playerDataManager.getWisdomTrait(playerUUID);
        int talentBonus1 = talentTreeManager.getTalentLevel(playerUUID, 1);
        int talentBonus2 = talentTreeManager.getTalentLevel(playerUUID, 9);
        int rankBonus = statsManager.getStatistics(playerUUID, 12);
        double archXPMul = playerDataManager.getArchXPMul(playerUUID);

        double baseMultiplier = ((traitBonus * 2 +
                talentBonus1 * 25 +
                talentBonus2 +
                rankBonus * 2) / 100.0) + archXPMul;

        double totalXP = baseMultiplier * blockEXP;
        int currentBonus = playerDataManager.getArchBonusXP(playerUUID);

        if (currentBonus <= 0) {
            return totalXP;
        }
        double bonusXP = Math.min(currentBonus, totalXP);

        if (bonusXP >= totalXP) {
            playerDataManager.reduceBonusXP(playerUUID, (int) totalXP);
        } else {
            playerDataManager.resetBonusXP(playerUUID);
        }
        return totalXP + bonusXP;
    }
}
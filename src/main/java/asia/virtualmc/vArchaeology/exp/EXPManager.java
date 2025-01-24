package asia.virtualmc.vArchaeology.exp;

import asia.virtualmc.vArchaeology.Main;
import asia.virtualmc.vArchaeology.storage.Statistics;
import asia.virtualmc.vArchaeology.storage.PlayerData;

import asia.virtualmc.vArchaeology.storage.TalentTree;
import asia.virtualmc.vArchaeology.utilities.EffectsUtil;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.text.DecimalFormat;

public class EXPManager {
    private final Main plugin;
    private final Statistics statistics;
    private final PlayerData playerData;
    private final TalentTree talentTree;
    private final EffectsUtil effectsUtil;
    private final Random random;
    private final ConcurrentMap<UUID, BlockBreakData> blockBreakDataMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, MaterialGetData> materialGetDataMap = new ConcurrentHashMap<>();

    public EXPManager(@NotNull Main plugin,
                      @NotNull Statistics statistics,
                      @NotNull PlayerData playerData,
                      @NotNull TalentTree talentTree,
                      EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.statistics = statistics;
        this.playerData = playerData;
        this.talentTree = talentTree;
        this.effectsUtil = effectsUtil;
        this.random = new Random();
    }

    private record BlockBreakData(double baseMultiplier, boolean hasTalentID12) { }
    private record MaterialGetData(double baseMultiplier, boolean hasTalentID14) { }

    public void precalculateEXPData(UUID playerUUID) {
        // Wisdom Trait: Block-break - 2% XP/level, Material-get - 1% XP/level
        int traitBonus = playerData.getWisdomTrait(playerUUID);
        // Rank Bonuses: 1% XP/level for both block-break and material-get
        int rankBonus = statistics.getStatistics(playerUUID, 1);
        // XP Multiplier - global multiplier
        double archXPMul = playerData.getArchXPMul(playerUUID);

        /* ------------------ Block-Break Data ------------------ */
        // Sagacity Talent
        int talentBonus1 = talentTree.getTalentLevel(playerUUID, 1);
        // Lucky Break Talent
        boolean hasTalentID12 = (talentTree.getTalentLevel(playerUUID, 12) == 1);

        // Precompute the base multiplier for block-break EXP
        double blockBreakBaseMultiplier =
                ((traitBonus * 2) + (talentBonus1 * 15) + (rankBonus)) / 100.0
                        + archXPMul;

        BlockBreakData blockBreakData = new BlockBreakData(
                blockBreakBaseMultiplier,
                hasTalentID12
        );
        blockBreakDataMap.put(playerUUID, blockBreakData);

        /* ------------------ Material-Get Data ----------------- */
        // Insightful Judgement
        int talentBonus2 = talentTree.getTalentLevel(playerUUID, 2);
        // Good Fortune
        boolean hasTalentID14 = (talentTree.getTalentLevel(playerUUID, 14) == 1);

        // Precompute the base multiplier for material-get EXP
        double materialGetBaseMultiplier =
                (traitBonus + (talentBonus2 * 2) + (rankBonus)) / 100.0
                        + archXPMul;

        MaterialGetData materialGetData = new MaterialGetData(
                materialGetBaseMultiplier,
                hasTalentID14
        );
        materialGetDataMap.put(playerUUID, materialGetData);
    }

    public void unloadPlayerEXPData(UUID playerUUID) {
        blockBreakDataMap.remove(playerUUID);
        materialGetDataMap.remove(playerUUID);
    }

    public double getTotalBlockBreakEXP(UUID playerUUID, float blockEXP) {
        BlockBreakData data = blockBreakDataMap.get(playerUUID);
        if (data == null) {
            precalculateEXPData(playerUUID);
            data = blockBreakDataMap.get(playerUUID);
            if (data == null) {
                return 0.0;
            }
        }

        // Check for Talent ID 12
        if (data.hasTalentID12() && random.nextInt(10) < 1) {
            blockEXP *= 2;
        }

        double totalXP = data.baseMultiplier() * blockEXP;
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
        return Math.ceil(totalXP + bonusXP);
    }

    public double getTotalMaterialGetEXP(UUID playerUUID, int dropTable) {
        MaterialGetData data = materialGetDataMap.get(playerUUID);
        if (data == null) {
            precalculateEXPData(playerUUID);
            data = materialGetDataMap.get(playerUUID);
            if (data == null) {
                return 0.0;
            }
        }

        // Material Base EXP from ID 1 to ID 7
        List<Integer> matBaseEXP = Arrays.asList(20, 35, 60, 80, 150, 300, 1250);
        int matEXP = matBaseEXP.get(dropTable - 1);

        // Check for Talent ID 14
        if (data.hasTalentID14() && random.nextInt(100) < 5) {
            matEXP *= 2;
        }

        double totalXP = data.baseMultiplier() * matEXP;
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
        return Math.ceil(totalXP + bonusXP);
    }

    public void addLampXP(UUID uuid, int lampType) {
        switch (lampType) {
            case 1 -> lampType = 500;
            case 2 -> lampType = 1000;
            case 3 -> lampType = 2000;
            case 4 -> lampType = 4000;
            default -> lampType = 0;
        }
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        double lampXPCalc = Math.pow(((float) playerData.getArchLevel(uuid) / 20), 2) * lampType;
        playerData.updateExp(uuid, lampXPCalc, "add");
        effectsUtil.sendPlayerMessage(uuid, "<green>You have received " + decimalFormat.format(lampXPCalc) + " Archaeology XP!");
        effectsUtil.playSoundUUID(uuid, "minecraft:entity.player.levelup", Sound.Source.PLAYER, 1.0f, 1.0f);
    }

    public void addStarXP(UUID uuid, int starType) {
        switch (starType) {
            case 1 -> starType = 500;
            case 2 -> starType = 1000;
            case 3 -> starType = 2000;
            case 4 -> starType = 4000;
            default -> starType = 0;
        }
        int bonusXPCalc = (int) (Math.pow((double) playerData.getArchLevel(uuid) / 20, 2) * starType);
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        playerData.addBonusXP(uuid, bonusXPCalc);
        effectsUtil.sendPlayerMessage(uuid, "<green>You have received " + decimalFormat.format(bonusXPCalc) + " Archaeology bonus XP!");
        effectsUtil.playSoundUUID(uuid, "minecraft:entity.player.levelup", Sound.Source.PLAYER, 1.0f, 1.0f);
    }
}
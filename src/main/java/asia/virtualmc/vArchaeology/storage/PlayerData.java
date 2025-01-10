package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.Main;
import org.jetbrains.annotations.NotNull;

public class PlayerData {
    private static final int MAX_EXP = 1_000_000_000;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 120;

    // playerStats
    private String name;
    private int archEXP;
    private int archLevel;
    private int archApt;
    private int archLuck;

    // internalStats
    private double archADP;
    private double archXPMul;
    private int archBonusXP;

    // achievements
    private int blocksMined;
    private int artefactsFound;
    private int artefactsRestored;
    private int treasuresFound;

    private final PlayerDataManager playerDataManager;

    public PlayerData(@NotNull PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        this.name = "";
        this.archLevel = MIN_LEVEL;
    }

    public PlayerData(@NotNull String name, int archEXP, int archLevel, int archApt, int archLuck,
                      double archADP, double archXPMul, int archBonusXP,
                      int blocksMined, int artefactsFound, int artefactsRestored, int treasuresFound
                      ) {
        this.playerDataManager = null;
        this.name = name;
        this.archEXP = archEXP;
        this.archLevel = archLevel;
        this.archApt = archApt;
        this.archLuck = archLuck;
        this.archADP = archADP;
        this.archXPMul = archXPMul;
        this.archBonusXP = archBonusXP;
        this.blocksMined = blocksMined;
        this.artefactsFound = artefactsFound;
        this.artefactsRestored = artefactsRestored;
        this.treasuresFound = treasuresFound;
    }

    // Data Retrieval Methods
    @NotNull
    public String getName() { return name; }
    public int getArchExp() { return archEXP; }
    public int getArchLevel() { return archLevel; }
    public int getArchApt() { return archApt; }
    public int getArchLuck() { return archLuck; }
    public double getArchADP() { return archADP; }
    public double getArchXPMul() { return archXPMul; }
    public int getArchBonusXP() { return archBonusXP; }
    public int getBlocksMined() { return blocksMined; }
    public int getArtefactsFound() { return artefactsFound; }
    public int getArtefactsRestored() { return artefactsRestored; }
    public int getTreasuresFound() { return treasuresFound; }

    // Data Manipulation Methods
    public void setArchEXP(int value) {
        this.archEXP = Math.max(0, Math.min(value, MAX_EXP));
    }

    public void addArchEXP(int value) {
        if (value <= 0) return;
        setArchEXP(Math.min(MAX_EXP, archEXP + value));
        checkAndApplyLevelUp();
    }

    public void subtractArchEXP(int value) {
        if (value <= 0) return;
        setArchEXP(Math.max(0, archEXP - value));
    }

    public void setArchLevel(int value) {
        this.archLevel = Math.max(MIN_LEVEL, Math.min(value, MAX_LEVEL));
    }

    public void addArchLevel(int value) {
        if (value <= 0) return;
        setArchLevel(archLevel + value);
    }

    public void subtractArchLevel(int value) {
        if (value <= 0) return;
        setArchLevel(archLevel - value);
    }

    public void incrementBlocksMined() { blocksMined++; }
    public void incrementArtefactsFound() { artefactsFound++; }
    public void incrementArtefactsRestored() { artefactsRestored++; }
    public void incrementTreasuresFound() { treasuresFound++; }

    public void checkAndApplyLevelUp() {
        if (playerDataManager == null) return;
        var experienceTable = playerDataManager.getExperienceTable();
        while (experienceTable.containsKey(archLevel + 1) &&
                archEXP >= experienceTable.get(archLevel + 1) && archLevel <= MAX_LEVEL) {
            addArchLevel(1);
        }
    }
}

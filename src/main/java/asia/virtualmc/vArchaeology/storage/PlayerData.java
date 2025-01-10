package asia.virtualmc.vArchaeology.storage;

import asia.virtualmc.vArchaeology.listeners.PlayerJoinManager;

public class PlayerData {
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

    public PlayerData(String name, int archEXP, int archLevel, int archApt, int archLuck,
                      double archADP, double archXPMul, int archBonusXP,
                      int blocksMined, int artefactsFound, int artefactsRestored, int treasuresFound
                      ) {
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
//    public void checkAndApplyLevelUp() {
//        while (experienceTable.containsKey(this.archLevel + 1) && this.archEXP >= experienceTable.get(this.archLevel + 1)) {
//            addArchLevel(1);
//            this.archEXP -= experienceTable.get(this.archLevel);
//        }
//    }

    public void setArchEXP(int value) {
        if (value < 0 || value > 1000000000) {
            this.archEXP = 0;
        } else {
            this.archEXP = value;
        }
    }
    public void addArchEXP(int value) {
        if (this.archEXP + value > 1000000000) {
            this.archEXP = 1000000000;
        } else {
            this.archEXP += value;
        }
    }
    public void subtractArchEXP(int value) {
        if (this.archEXP - value < 0) {
            this.archEXP = 0;
        } else {
            this.archEXP -= value;
        }
    }

    public void setArchLevel(int value) {
        if (value < 1 || value > 120) {
            this.archLevel = 1;
        } else {
            this.archLevel = value;
        }
    }
    public void addArchLevel(int value) {
        if (this.archLevel + value > 120) {
            this.archLevel = 120;
        } else {
            this.archLevel += value;
        }
    }
    public void subtractArchLevel(int value) {
        if (this.archLevel - value < 1) {
            this.archLevel = 1;
        } else {
            this.archLevel -= value;
        }
    }
}

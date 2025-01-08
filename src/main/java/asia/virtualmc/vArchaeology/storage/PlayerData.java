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
    public void setName(String value) { this.name = value; }
    public void setArchLevel(int value) { this.archLevel = value; }
    public void addArchLevel(int value) { this.archLevel += value; }
    public void subtractArchLevel(int value) { this.archLevel += value; }


    public void setExp(int exp) { this.archADP = exp; }


    public void setLevel(int level) { this.level = level; }

    public void setBreakChance(double breakChance) { this.breakChance = breakChance; }

    public void setGatherRate(double gatherRate) { this.gatherRate = gatherRate; }
}

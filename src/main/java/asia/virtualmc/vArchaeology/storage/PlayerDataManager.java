package asia.virtualmc.vArchaeology.storage;

import java.math.BigDecimal;
import java.util.UUID;

public class PlayerDataManager {

    private final UUID playerUUID;
    private final String playerName;
    private BigDecimal playerArchEXP;
    private int playerArchLevel;
    private int playerArchApt;
    private int playerArchLuck;

    public PlayerDataManager(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.playerArchEXP = BigDecimal.ZERO;
        this.playerArchLevel = 0;
        this.playerArchApt = 0;
        this.playerArchLuck = 0;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public BigDecimal getPlayerArchEXP() {
        return playerArchEXP;
    }

    public int getPlayerArchLevel() {
        return playerArchLevel;
    }

    public int getPlayerArchApt() {
        return playerArchApt;
    }

    public int getPlayerArchLuck() {
        return playerArchLuck;
    }

    public void setPlayerArchEXP(BigDecimal playerArchEXP) {
        this.playerArchEXP = playerArchEXP;
    }

    public void setPlayerArchLevel(int playerArchLevel) {
        this.playerArchLevel = playerArchLevel;
    }

    public void setPlayerArchApt(int playerArchApt) {
        this.playerArchApt = playerArchApt;
    }

    public void setPlayerArchLuck(int playerArchLuck) {
        this.playerArchLuck = playerArchLuck;
    }
}


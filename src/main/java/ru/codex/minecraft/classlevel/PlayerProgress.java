package ru.codex.minecraft.classlevel;

public class PlayerProgress {
    private PlayerClass playerClass;
    private int level;
    private int xp;

    public PlayerProgress(PlayerClass playerClass, int level, int xp) {
        this.playerClass = playerClass;
        this.level = level;
        this.xp = xp;
    }

    public static PlayerProgress empty() {
        return new PlayerProgress(null, 1, 0);
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}

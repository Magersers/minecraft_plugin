package ru.codex.minecraft.classlevel;

public class PlayerProgress {
    private PlayerClass playerClass;
    private int level;
    private int xp;

    private CombatClass combatClass;
    private int combatLevel;
    private int combatXp;

    public PlayerProgress(PlayerClass playerClass, int level, int xp, CombatClass combatClass, int combatLevel, int combatXp) {
        this.playerClass = playerClass;
        this.level = level;
        this.xp = xp;
        this.combatClass = combatClass;
        this.combatLevel = combatLevel;
        this.combatXp = combatXp;
    }

    public static PlayerProgress empty() {
        return new PlayerProgress(null, 1, 0, null, 1, 0);
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

    public CombatClass getCombatClass() {
        return combatClass;
    }

    public void setCombatClass(CombatClass combatClass) {
        this.combatClass = combatClass;
    }

    public int getCombatLevel() {
        return combatLevel;
    }

    public void setCombatLevel(int combatLevel) {
        this.combatLevel = combatLevel;
    }

    public int getCombatXp() {
        return combatXp;
    }

    public void setCombatXp(int combatXp) {
        this.combatXp = combatXp;
    }
}

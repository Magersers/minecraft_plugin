package ru.codex.minecraft.classlevel;

public enum CombatClass {
    WARRIOR("Воин"),
    ARCHER("Лучник"),
    TANK("Танк");

    private final String displayName;

    CombatClass(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}


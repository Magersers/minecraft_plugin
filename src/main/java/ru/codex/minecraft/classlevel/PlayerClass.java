package ru.codex.minecraft.classlevel;

public enum PlayerClass {
    MINER("Шахтер");

    private final String displayName;

    PlayerClass(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

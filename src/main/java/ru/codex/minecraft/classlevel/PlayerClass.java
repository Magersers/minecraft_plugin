package ru.codex.minecraft.classlevel;

public enum PlayerClass {
    HAPPY_MINER("Счастливый шахтёр"),
    BLACKSMITH("Кузнец");

    private final String displayName;

    PlayerClass(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

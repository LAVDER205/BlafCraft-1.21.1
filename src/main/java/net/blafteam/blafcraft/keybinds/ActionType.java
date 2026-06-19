package net.blafteam.blafcraft.keybinds;

public enum ActionType {
    SPEED(100),        // 5 секунд
    JUMP(100),         // 5 секунд
    ARROW(20),   // 1 секунда
    SNOWBALL(10);      // 0.5 секунды

    private final int cooldownTicks;

    ActionType(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
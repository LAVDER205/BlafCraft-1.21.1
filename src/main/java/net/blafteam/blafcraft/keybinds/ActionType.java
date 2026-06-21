package net.blafteam.blafcraft.keybinds;

public enum ActionType {
    SPEED(100, 1),
    JUMP(100, 2),
    ARROW(20, 3),
    SNOWBALL(10, 4);

    private final int cooldownTicks;
    private final int price;

    ActionType(int cooldownTicks, int price) {
        this.cooldownTicks = cooldownTicks;
        this.price = price;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getPrice() {
        return price;
    }
}
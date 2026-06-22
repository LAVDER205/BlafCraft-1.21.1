package net.blafteam.blafcraft.keybinds;

public enum ActionType {
    ARROW(40, 1),
    FIREBALL(80, 2),
    CREATION_STEP(0, 0);

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
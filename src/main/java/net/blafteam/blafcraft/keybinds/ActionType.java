package net.blafteam.blafcraft.keybinds;

public enum ActionType {
    ARROW(40, 1),
    FIREBALL(80, 2),
    CREATION_STEP(0, 0),
    TELEPORT_DASH(0, 0);

    private final int cooldownTicks;
    private final int exp_price;

    ActionType(int cooldownTicks, int price) {
        this.cooldownTicks = cooldownTicks;
        this.exp_price = price;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getExp_price() {
        return exp_price;
    }
}
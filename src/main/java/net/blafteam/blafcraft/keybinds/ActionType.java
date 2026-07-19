package net.blafteam.blafcraft.keybinds;

public enum ActionType {
    TEST(60, 0, 0),
    ARROW(40, 1, 0),
    FIREBALL(80, 2, 0),
    CREATION_STEP(0, 0, 0),
    TELEPORT_DASH(2, 0, 10),
    TELEPORT_GLANCE(20, 0, 40),
    BLOODLUST(0, 0, 0);

    private final int cooldownTicks;
    private final int exp_price;
    private final int mana_price;

    ActionType(int cooldownTicks, int exp_price, int mana_price) {
        this.cooldownTicks = cooldownTicks;
        this.exp_price = exp_price;
        this.mana_price = mana_price;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getExp_price() {
        return exp_price;
    }

    public int getMana_price() {
        return mana_price;
    }
}
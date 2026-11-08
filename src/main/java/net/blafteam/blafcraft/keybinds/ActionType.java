package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.feature.frynote.FrynoteBalance;

public enum ActionType {
    TEST(60, 0, 0),
    ARROW(40, 1, 0),
    FIREBALL(80, 2, 0),
    CREATION_STEP(0, 0, 0),
    TELEPORT_DASH(FrynoteBalance.CHARGE_TICKS, 0, FrynoteBalance.DASH_MANA),
    TELEPORT_GLANCE(FrynoteBalance.GLANCE_COOLDOWN, 0, FrynoteBalance.GLANCE_MANA),
    TELEPORT_ANCHOR(FrynoteBalance.ANCHOR_COOLDOWN, 0, FrynoteBalance.ANCHOR_SET_MANA),
    BLOODLUST(0, 0, 0),
    TIME_BOMB(400, 0, 0),
    FLIGHT(200,0,0),
    SCULK_MARK(1200,0,0),
    MORPH(6000,0,0),
    OVERDOSE_ABILITY(1,0,0);

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

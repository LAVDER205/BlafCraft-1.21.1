package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.feature.frigby.FrigbyBalance;
import net.blafteam.blafcraft.feature.frynote.FrynoteBalance;

public enum ActionType {
    TEST(60, 0, 0),
    ARROW(40, FrigbyBalance.ARROW_VOLLEY_XP, 0),
    FIREBALL(80, FrigbyBalance.FIREBALL_XP, 0),
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
    private final int experiencePointCost;
    private final int mana_price;

    ActionType(int cooldownTicks, int experiencePointCost, int mana_price) {
        this.cooldownTicks = cooldownTicks;
        this.experiencePointCost = experiencePointCost;
        this.mana_price = mana_price;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getExperiencePointCost() {
        return experiencePointCost;
    }

    public int getMana_price() {
        return mana_price;
    }
}

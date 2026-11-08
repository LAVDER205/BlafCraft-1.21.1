package net.blafteam.blafcraft.feature.frynote;

/** Стартовые значения для игровых проверок. Время задано в тиках: 20 тиков = 1 секунда. */
public final class FrynoteBalance {
    public static final int MAX_CHARGES = 3;
    public static final int CHARGE_TICKS = 4 * 20;
    public static final int DASH_MANA = 10;
    public static final double DASH_DISTANCE = 5;
    public static final int GLANCE_MANA = 30;
    public static final int GLANCE_COOLDOWN = 8 * 20;
    public static final double GLANCE_DISTANCE = 24;
    public static final int ANCHOR_SET_MANA = 10;
    public static final int ANCHOR_RETURN_MANA = 15;
    public static final int ANCHOR_LIFETIME = 8 * 20;
    public static final int ANCHOR_COOLDOWN = 16 * 20;
    public static final double ANCHOR_DISTANCE = 24;
    public static final int COMBAT_TICKS = 6 * 20;
    public static final float COMBAT_REGEN = 2.0F / 20;
    public static final float REST_REGEN = 5.0F / 20;
    public static final int MANEUVER_WINDOW = 2 * 20;
    public static final int BONUS_COOLDOWN = 3 * 20;
    public static final int BONUS_MANA = 3;
    // Скалярное произведение <= 0 соответствует углу не меньше 90 градусов.
    public static final double SIDE_CHANGE_DOT = 0;

    private FrynoteBalance() {}
}

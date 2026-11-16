package net.blafteam.blafcraft.feature.frigby;

/** Стартовые цены для проверки в игре. Все значения в очках опыта. */
public final class FrigbyBalance {
    public static final int ARROW_VOLLEY_XP = 10;
    // Между стрелами проходит окно защиты цели от повторного попадания (10 тиков).
    public static final int ARROW_INTERVAL_TICKS = 12;
    public static final int FIREBALL_XP = 20;
    public static final int STEP_XP_PER_TICK = 1; // 20 очков за секунду на воздушной опоре.

    public static final int KING_CALL_DURATION = 10 * 20;
    public static final int KING_CALL_COOLDOWN = 5 * 60 * 20;

    public static final int RELIC_CAPACITY = 120;
    public static final int RELIC_STARTUP_TICKS = 10;
    public static final int RELIC_TRANSFER_INTERVAL = 2;
    // Скорость зарядки в сотых долях очка за тик.
    public static final int RELIC_BASE_CHARGE_RATE = 8;
    public static final int RELIC_MIN_CHARGE_RATE = 1;
    public static final int RELIC_LEVELS_PER_SLOWDOWN = 4;

    private FrigbyBalance() {}
}

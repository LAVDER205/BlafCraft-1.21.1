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

    public static final int LOADOUT_LOCK_TICKS = 10 * 20;
    public static final int BUILD_RANGE = 6;
    public static final int WALL_XP = 25;
    public static final int WALL_COOLDOWN = 12 * 20;
    public static final int WALL_DURATION = 6 * 20;

    public static final int RAIN_XP = 35;
    public static final int RAIN_COOLDOWN = 18 * 20;
    public static final int RAIN_RANGE = 24;
    public static final int RAIN_RADIUS = 4;
    public static final int RAIN_WARNING_TICKS = 24;
    public static final int RAIN_DURATION_TICKS = 3 * 20;

    public static final int CANNON_XP = 45;
    public static final int CANNON_COOLDOWN = 25 * 20;
    public static final int CANNON_WINDUP = 2 * 20;
    public static final double CANNON_SHOT_RANGE = 30.0;
    public static final int CANNON_MAX_FLIGHT_TICKS = 40; // Запасной взрыв, если снаряд замедлился.
    public static final float CANNON_POWER = 3.2F;

    private FrigbyBalance() {}
}

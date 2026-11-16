package net.blafteam.blafcraft.feature.frigby;

/** Стартовые цены для проверки в игре. Все значения в очках опыта. */
public final class FrigbyBalance {
    public static final int ARROW_VOLLEY_XP = 7;
    // Между стрелами проходит окно защиты цели от повторного попадания (10 тиков).
    public static final int ARROW_INTERVAL_TICKS = 12;
    public static final int FIREBALL_XP = 16;
    public static final int STEP_XP_PER_TICK = 1; // 20 очков за секунду на воздушной опоре.

    private FrigbyBalance() {}
}

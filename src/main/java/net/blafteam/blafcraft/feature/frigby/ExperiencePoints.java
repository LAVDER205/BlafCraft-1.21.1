package net.blafteam.blafcraft.feature.frigby;

import net.minecraft.server.level.ServerPlayer;

public final class ExperiencePoints {
    private ExperiencePoints() {}

    public static boolean canAfford(ServerPlayer player, int cost) {
        if (cost == 0) return true;

        // Зачарование и команды могут менять уровень без обновления totalExperience.
        // Считаем очки по текущему уровню и заполнению видимой полосы.
        long level = player.experienceLevel;
        int pointsInLevel = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        long completedLevels;
        if (level <= 16) {
            completedLevels = level * (level + 6);
        } else if (level <= 31) {
            completedLevels = (5 * level * level - 81 * level + 720) / 2;
        } else {
            completedLevels = (9 * level * level - 325 * level + 4440) / 2;
        }
        return completedLevels + pointsInLevel >= cost;
    }
}

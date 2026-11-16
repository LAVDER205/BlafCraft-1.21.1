package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.ClientCooldowns;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ConstructionPreview {
    private static final DustParticleOptions VALID = new DustParticleOptions(new Vector3f(0.25F, 1.0F, 0.35F), 0.85F);
    private static final DustParticleOptions INVALID = new DustParticleOptions(new Vector3f(1.0F, 0.18F, 0.18F), 0.85F);

    private ConstructionPreview() {}

    public static boolean canCast(LocalPlayer player, ConstructionAim aim) {
        return aim != null && aim.isClear(player) && ClientCooldowns.getRemaining(aim.action()) == 0
                && ExperiencePoints.canAfford(player, aim.action().getExperiencePointCost());
    }

    public static void draw(ClientLevel level, LocalPlayer player, ConstructionAim aim) {
        if (aim == null || level.getGameTime() % 3 != 0) return;
        // Добавляем частицы локально. Сервер о предпросмотре не знает, другие игроки его не видят.
        var dust = canCast(player, aim) ? VALID : INVALID;
        if (aim.action() == ActionType.ARROW_RAIN) {
            for (Vec3 point : ConstructionAim.rainBoundary(level, aim.groundCenter())) dot(level, dust, point);
            return;
        }

        for (BlockPos pos : aim.blocks()) {
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) dot(level, dust, Vec3.atLowerCornerOf(pos).add(x, y + 0.03, z));
                }
            }
        }
        if (aim.action() == ActionType.CANNON) {
            Vec3 center = Vec3.atCenterOf(aim.anchor());
            Vec3 direction = Vec3.atLowerCornerOf(aim.facing().getNormal());
            for (int i = 1; i <= 8; i++) dot(level, dust, center.add(direction.scale(i * 0.5)));
        }
    }

    private static void dot(ClientLevel level, DustParticleOptions dust, Vec3 point) {
        level.addParticle(dust, point.x, point.y, point.z, 0, 0, 0);
    }
}

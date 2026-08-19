package net.blafteam.blafcraft.entity.custom;

import net.blafteam.blafcraft.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LargeArtilleryFireballEntity extends LargeFireball {
    private static final double ASCEND_SPEED = 1.2;
    private static final double DESCEND_SPEED = 2.5;
    private final int ticksBeforeTargeting = 20;
    private LivingEntity target;
    private final int explosionPower = 3; // сила взрыва

    // Конструктор для регистрации EntityType
    public LargeArtilleryFireballEntity(EntityType<? extends LargeFireball> type, Level level) {
        super(type, level);
    }

    // Конструктор для создания снаряда в мире
    public LargeArtilleryFireballEntity(Level level, LivingEntity target, double x, double y, double z) {
        this(ModEntities.ARTILLERY_LARGE_FIREBALL.get(), level);
        this.setPos(x, y, z);
        this.target = target;
        this.setDeltaMovement(new Vec3(0, ASCEND_SPEED, 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= ticksBeforeTargeting) {
            if (this.target != null && this.target.isAlive()) {
                Vec3 selfPos = this.position();
                Vec3 targetPos = this.target.getEyePosition(1.0F);
                Vec3 direction = targetPos.subtract(selfPos).normalize();
                this.setDeltaMovement(direction.scale(DESCEND_SPEED));
            }
        }
    }
}
package net.blafteam.blafcraft.entity.custom;

import net.blafteam.blafcraft.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class LargeArtilleryFireballEntity extends SmallFireball {
    private static final double ASCEND_SPEED = 10;
    private static final double DESCEND_SPEED = 3;
    private static final float EXPLOSION_POWER = 2.0F;

    private int ticksBeforeTargeting = 20; // сколько тиков лететь вверх
    private LivingEntity target;

    public LargeArtilleryFireballEntity(EntityType<? extends SmallFireball> type, Level level) {
        super(type, level);
    }

    public LargeArtilleryFireballEntity(Level level, LivingEntity target, double x, double y, double z) {
        super(level, x, y, z, new Vec3(0.0D, ASCEND_SPEED, 0.0D)); // вектор скорости вверх
        this.target = target;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result); // сохраняет урон и поджог
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
            this.discard(); // удаляем снаряд после взрыва
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result); // может вызвать поджог блока
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.tickCount >= ticksBeforeTargeting) {
                if (this.target != null && this.target.isAlive()) {
                    Vec3 selfPos = this.position();
                    Vec3 targetPos = this.target.getEyePosition(1.0F);
                    Vec3 direction = targetPos.subtract(selfPos).normalize();
                    this.setDeltaMovement(direction.scale(DESCEND_SPEED)); // скорость падения на цель
                }
            }
        }
    }
}
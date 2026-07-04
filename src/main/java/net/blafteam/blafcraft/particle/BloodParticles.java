package net.blafteam.blafcraft.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class BloodParticles extends TextureSheetParticle {
    protected BloodParticles(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.lifetime = 60;
        this.quadSize = 0.1f;
        this.setSpriteFromAge(spriteSet);

        this.setColor(0.6f, 0.0f, 0.0f);  // тёмно-красный
        this.hasPhysics = true;
        this.gravity = 0.5f;
        this.friction = 0.98f;
        this.roll = (float) (Math.random() * Math.PI * 2);
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        // Плавное уменьшение прозрачности к концу жизни
        this.alpha = 1.0f - (float) this.age / this.lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new BloodParticles(level, pX, pY, pZ, this.spriteSet, pXSpeed / 2, pYSpeed - 1, pZSpeed / 2);
        }
    }
}

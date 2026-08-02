package net.blafteam.blafcraft.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.jetbrains.annotations.Nullable;

public class SparkleParticles extends TextureSheetParticle {
    public SparkleParticles(ClientLevel level, double x, double y, double z,
                            SpriteSet spriteSet,
                            double xSpeed, double ySpeed, double zSpeed,
                            SparkleParticleOptions options) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.setSpriteFromAge(spriteSet);

        this.quadSize = 0.05f;
        this.friction = 0.9f;
        this.lifetime = 20;
        this.roll = (float) (Math.random() * Math.PI * 2);
        this.oRoll = this.roll;

        this.setColor(options.r(), options.g(), options.b());
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = (float) (1 - Math.pow((double) this.age / this.lifetime, 8));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SparkleParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public @Nullable Particle createParticle(SparkleParticleOptions options, ClientLevel level, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new SparkleParticles(level, pX, pY, pZ, this.spriteSet, pXSpeed, pYSpeed, pZSpeed, options);
        }
    }
}
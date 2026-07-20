package net.blafteam.blafcraft.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class PetalParticles extends TextureSheetParticle {
    protected PetalParticles(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.lifetime = 160;
        this.quadSize = 0.15f;
        this.setSpriteFromAge(spriteSet);

        this.setColor(0.4f, 0.0f, 0.4f);  // тёмно-красный
        this.hasPhysics = true;
        this.gravity = 0.2f;
        this.friction = 0.98f;
        this.roll = (float) (Math.random() * Math.PI * 2);
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) this.alpha = this.alpha - 0.1f;
        else this.alpha = (float) Math.sin((double) this.age /this.lifetime * Math.PI) * 2;

        if (this.alpha > 1) this.alpha = 1.0f;
        if (this.alpha < 0) this.alpha = 0;
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
            return new PetalParticles(level, pX, pY, pZ, this.spriteSet, pXSpeed / 5, pYSpeed - 1, pZSpeed / 5);
        }
    }
}

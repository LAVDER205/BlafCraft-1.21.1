package net.blafteam.blafcraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record SparkleParticleOptions(float r, float g, float b) implements ParticleOptions {

    public static final Codec<SparkleParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("r").forGetter(SparkleParticleOptions::r),
                    Codec.FLOAT.fieldOf("g").forGetter(SparkleParticleOptions::g),
                    Codec.FLOAT.fieldOf("b").forGetter(SparkleParticleOptions::b)
            ).apply(instance, SparkleParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SparkleParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, opts) -> {
                buf.writeFloat(opts.r);
                buf.writeFloat(opts.g);
                buf.writeFloat(opts.b);
            },
            buf -> new SparkleParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat())
    );

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPARKLE_PARTICLES.get();
    }
}

package net.blafteam.blafcraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;

public record PetalParticleOptions(float r, float g, float b) implements ParticleOptions {

    // Сериализатор в JSON/команды (здесь не обязателен, но нужен для реестра)
    public static final Codec<PetalParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("r").forGetter(PetalParticleOptions::r),
                    Codec.FLOAT.fieldOf("g").forGetter(PetalParticleOptions::g),
                    Codec.FLOAT.fieldOf("b").forGetter(PetalParticleOptions::b)
            ).apply(instance, PetalParticleOptions::new)
    );

    @Override
    public ParticleType<?> getType() {
        return ModParticles.PETAL_PARTICLES.get();
    }

    // StreamCodec для сетевой передачи
    public static final StreamCodec<ByteBuf, PetalParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, opts) -> {
                buf.writeFloat(opts.r);
                buf.writeFloat(opts.g);
                buf.writeFloat(opts.b);
            },
            buf -> new PetalParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat())
    );
}

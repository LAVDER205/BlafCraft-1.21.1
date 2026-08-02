package net.blafteam.blafcraft.particle;

import com.mojang.serialization.MapCodec;
import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, BlafCraft.MODID);

    public static final Supplier<SimpleParticleType> TELEPORT_PARTICLES =
            PARTICLE_TYPES.register("teleport_particles", () -> new SimpleParticleType(true));

    public static final Supplier<SimpleParticleType> BLOOD_PARTICLES =
            PARTICLE_TYPES.register("blood_particles", () -> new SimpleParticleType(false));

    public static final Supplier<ParticleType<SparkleParticleOptions>> SPARKLE_PARTICLES =
            PARTICLE_TYPES.register("sparkle_particles", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<SparkleParticleOptions> codec() {
                    return SparkleParticleOptions.CODEC.fieldOf("options").xmap(o -> o, o -> o);
                }
                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, SparkleParticleOptions> streamCodec() {
                    return SparkleParticleOptions.STREAM_CODEC;
                }
            });

    public static final Supplier<ParticleType<PetalParticleOptions>> PETAL_PARTICLES =
            PARTICLE_TYPES.register("petal_particles", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<PetalParticleOptions> codec() {
                    return PetalParticleOptions.CODEC.fieldOf("options").xmap(o -> o, o -> o);
                }
                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, PetalParticleOptions> streamCodec() {
                    return PetalParticleOptions.STREAM_CODEC;
                }
            });

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}

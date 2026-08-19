package net.blafteam.blafcraft.entity;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.entity.custom.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, BlafCraft.MODID);

    public static final Supplier<EntityType<GeckoEntity>> GECKO =
            ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.75f, 0.35f).build("gecko")); // size of the hitbox

    public static final Supplier<EntityType<TomahawkProjectileEntity>> TOMAHAWK =
            ENTITY_TYPES.register("tomahawk", () -> EntityType.Builder.<TomahawkProjectileEntity>of(TomahawkProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.15f).build("tomahawk"));

    public static final Supplier<EntityType<ChairEntity>> CHAIR_ENTITY =
            ENTITY_TYPES.register("chair_entity", () -> EntityType.Builder.of(ChairEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).build("chair_entity")); // size of the hitbox

    public static final Supplier<EntityType<SmallArtilleryFireballEntity>> ARTILLERY_FIREBALL =
            ENTITY_TYPES.register("artillery_fireball",
                    () -> EntityType.Builder.<SmallArtilleryFireballEntity>of(SmallArtilleryFireballEntity::new, MobCategory.MISC)
                            .sized(0.3125F, 0.3125F)
                            .build("artillery_fireball"));

    public static final Supplier<EntityType<LargeArtilleryFireballEntity>> ARTILLERY_LARGE_FIREBALL =
            ENTITY_TYPES.register("artillery_large_fireball",
                    () -> EntityType.Builder.<LargeArtilleryFireballEntity>of(LargeArtilleryFireballEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .build("artillery_large_fireball"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

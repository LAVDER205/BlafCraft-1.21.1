package net.blafteam.blafcraft;

import net.blafteam.blafcraft.block.ModBlocks;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.entity.ModEntities;
import net.blafteam.blafcraft.entity.client.GeckoRenderer;
import net.blafteam.blafcraft.entity.client.TomahawkProjectileRenderer;
import net.blafteam.blafcraft.item.ModCreativeModeTabs;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.particle.BloodParticles;
import net.blafteam.blafcraft.particle.ModParticles;
import net.blafteam.blafcraft.particle.TeleportParticles;
import net.blafteam.blafcraft.potion.ModPotions;
import net.blafteam.blafcraft.sound.ModSounds;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BlafCraft.MODID)
public class BlafCraft {
    public static final String MODID = "blafcraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public BlafCraft(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);

        ModParticles.PARTICLE_TYPES.register(modEventBus);

        ModPotions.register(modEventBus);

        ModEntities.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) { // creative vanilla tabs
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BISMUTH);
            event.accept(ModItems.RAW_BISMUTH);
        }
        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.BISMUTH_BLOCK);
            event.accept(ModBlocks.BISMUTH_ORE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.GECKO.get(), GeckoRenderer::new);
            EntityRenderers.register(ModEntities.TOMAHAWK.get(), TomahawkProjectileRenderer::new);

        }
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.TELEPORT_PARTICLES.get(), TeleportParticles.Provider::new);
            event.registerSpriteSet(ModParticles.BLOOD_PARTICLES.get(), BloodParticles.Provider::new);
        }
    }
}

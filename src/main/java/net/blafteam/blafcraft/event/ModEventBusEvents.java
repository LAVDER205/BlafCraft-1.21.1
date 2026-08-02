package net.blafteam.blafcraft.event;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModDataComponents;
import net.blafteam.blafcraft.entity.ModEntities;
import net.blafteam.blafcraft.entity.client.GeckoModel;
import net.blafteam.blafcraft.entity.client.TomahawkProjectileModel;
import net.blafteam.blafcraft.entity.custom.GeckoEntity;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GeckoModel.LAYER_LOCATION, GeckoModel::createBodyLayer);
        event.registerLayerDefinition(TomahawkProjectileModel.LAYER_LOCATION, TomahawkProjectileModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GECKO.get(), GeckoEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        // Сопоставляем каждый предмет с его цветом
        setFactonColor(event, ModItems.RED_FACTON.get(), DyeColor.RED);
        setFactonColor(event, ModItems.ORANGE_FACTON.get(), DyeColor.ORANGE);
        setFactonColor(event, ModItems.YELLOW_FACTON.get(), DyeColor.YELLOW);
        setFactonColor(event, ModItems.GREEN_FACTON.get(), DyeColor.GREEN);
        setFactonColor(event, ModItems.BLUE_FACTON.get(), DyeColor.BLUE);
        setFactonColor(event, ModItems.PURPLE_FACTON.get(), DyeColor.PURPLE);
        setFactonColor(event, ModItems.PINK_FACTON.get(), DyeColor.PINK);
        setFactonColor(event, ModItems.LIGHT_FACTON.get(), DyeColor.LIGHT_GRAY);
        setFactonColor(event, ModItems.DARK_FACTON.get(), DyeColor.BLACK);
    }

    private static void setFactonColor(ModifyDefaultComponentsEvent event, Item item, DyeColor color) {
        event.modify(item, builder -> builder.set(ModDataComponents.COLOR.get(), color));
    }
}

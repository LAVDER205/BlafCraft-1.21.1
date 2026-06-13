package net.blafteam.blafcraft.item;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.item.custom.ChiselItem;
import net.blafteam.blafcraft.item.custom.FuelItem;
import net.blafteam.blafcraft.item.custom.HammerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlafCraft.MODID);

    public static final DeferredItem<Item> BISMUTH = ITEMS.register("bismuth",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_BISMUTH = ITEMS.register("raw_bismuth",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(32)));

    public static final DeferredItem<Item> RADISH = ITEMS.register("radish",
            () -> new Item(new Item.Properties().food(ModFoodProperties.RADISH)) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.blafcraft.radish.tooltip"));
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final DeferredItem<Item> FROSTFIRE_ICE = ITEMS.register("frostfire_ice",
            () -> new FuelItem(new Item.Properties(), 800));
    public static final DeferredItem<Item> STARLIGHT_ASHES = ITEMS.register("starlight_ashes",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<SwordItem> BISMUTH_SWORD = ITEMS.register("bismuth_sword",
            () -> new SwordItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.BISMUTH, 5, -2.4f))));
    public static final DeferredItem<PickaxeItem> BISMUTH_PICKAXE = ITEMS.register("bismuth_pickaxe",
            () -> new PickaxeItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.BISMUTH, 1.0F, -2.8f))));
    public static final DeferredItem<ShovelItem> BISMUTH_SHOVEL = ITEMS.register("bismuth_shovel",
            () -> new ShovelItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.BISMUTH, 1.5F, -3.0f))));
    public static final DeferredItem<AxeItem> BISMUTH_AXE = ITEMS.register("bismuth_axe",
            () -> new AxeItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.BISMUTH, 6.0F, -3.2f))));
    public static final DeferredItem<HoeItem> BISMUTH_HOE = ITEMS.register("bismuth_hoe",
            () -> new HoeItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTiers.BISMUTH, 0F, -3.0f))));

    public static final DeferredItem<HammerItem> BISMUTH_HAMMER = ITEMS.register("bismuth_hammer",
            () -> new HammerItem(ModToolTiers.BISMUTH, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.BISMUTH, 7F, -3.5f))));

    public static final DeferredItem<ArmorItem> BISMUTH_HELMET = ITEMS.register("bismuth_helmet",
            () -> new ArmorItem(ModArmorMaterials.BISMUTH_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19))));
    public static final DeferredItem<ArmorItem> BISMUTH_CHESTPLATE = ITEMS.register("bismuth_chestplate",
            () -> new ArmorItem(ModArmorMaterials.BISMUTH_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19))));
    public static final DeferredItem<ArmorItem> BISMUTH_LEGGINGS = ITEMS.register("bismuth_leggings",
            () -> new ArmorItem(ModArmorMaterials.BISMUTH_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19))));
    public static final DeferredItem<ArmorItem> BISMUTH_BOOTS = ITEMS.register("bismuth_boots",
            () -> new ArmorItem(ModArmorMaterials.BISMUTH_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19))));

// Actual mod
    public static final DeferredItem<Item> SCULK_INGOT = ITEMS.register("sculk_ingot",
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_SCULK = ITEMS.register("raw_sculk",
        () -> new Item(new Item.Properties()));

    public static final DeferredItem<SwordItem> SCULK_SWORD = ITEMS.register("sculk_sword",
            () -> new SwordItem(ModToolTiers.SCULK, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.SCULK, 5, -2.4f))));
//    public static final DeferredItem<PickaxeItem> SCULK_PICKAXE = ITEMS.register("sculk_pickaxe",
//            () -> new PickaxeItem(ModToolTiers.SCULK, new Item.Properties()
//                    .attributes(PickaxeItem.createAttributes(ModToolTiers.SCULK, 1.0F, -2.8f))));
//    public static final DeferredItem<ShovelItem> SCULK_SHOVEL = ITEMS.register("sculk_shovel",
//            () -> new ShovelItem(ModToolTiers.SCULK, new Item.Properties()
//                    .attributes(ShovelItem.createAttributes(ModToolTiers.SCULK, 1.5F, -3.0f))));
    public static final DeferredItem<AxeItem> SCULK_AXE = ITEMS.register("sculk_axe",
            () -> new AxeItem(ModToolTiers.SCULK, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.SCULK, 6.0F, -3.2f))));
//    public static final DeferredItem<HoeItem> SCULK_HOE = ITEMS.register("sculk_hoe",
//            () -> new HoeItem(ModToolTiers.SCULK, new Item.Properties()
//                    .attributes(HoeItem.createAttributes(ModToolTiers.SCULK, 0F, -3.0f))));

//    public static final DeferredItem<ArmorItem> CREATION_HELMET = ITEMS.register("creation_helmet",
//            () -> new ArmorItem(ModArmorMaterials.CREATION_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
//                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(42))));
//    public static final DeferredItem<ArmorItem> CREATION_CHESTPLATE = ITEMS.register("creation_chestplate",
//            () -> new ArmorItem(ModArmorMaterials.CREATION_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
//                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(42))));
//    public static final DeferredItem<ArmorItem> CREATION_LEGGINGS = ITEMS.register("creation_leggings",
//            () -> new ArmorItem(ModArmorMaterials.CREATION_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
//                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(42))));
//    public static final DeferredItem<ArmorItem> CREATION_BOOTS = ITEMS.register("creation_boots",
//            () -> new ArmorItem(ModArmorMaterials.CREATION_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
//                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(42))));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

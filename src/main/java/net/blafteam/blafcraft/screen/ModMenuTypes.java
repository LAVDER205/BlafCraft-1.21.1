package net.blafteam.blafcraft.screen;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.screen.custom.RealizerMenu;
import net.blafteam.blafcraft.screen.custom.RuneMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BlafCraft.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RealizerMenu>> REALIZER_MENU =
            registerMenuType("realizer_menu", RealizerMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<RuneMenu>> RUNE_ACTION_MENU =
            registerMenuType("rune_block_menu", RuneMenu::new);

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                               IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

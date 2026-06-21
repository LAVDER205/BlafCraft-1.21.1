package net.blafteam.blafcraft.hud;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.ClientCooldowns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ModHUDs {

    @SubscribeEvent
    public static void onRender(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // В 1.21 GuiGraphics получаем так:
        GuiGraphics graphics = event.getGuiGraphics();

        int x = 20;
        int y = 20;

        // ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "textures/gui/speed.png");
        // graphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);

        for (ActionType action : ActionType.values()) {

            int remain = ClientCooldowns.getRemaining(action);

            if (remain > 0) {
                String text = action.name() + ": " + String.format("%.1f", remain / 20.0);
                graphics.drawString(mc.font, text, x + 16, y, 0xFFFFFF);
                y += 10; // сдвиг чтоб не было наслоения
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        ClientCooldowns.tick();
    }
}

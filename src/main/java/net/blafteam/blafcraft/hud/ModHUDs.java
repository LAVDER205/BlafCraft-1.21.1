package net.blafteam.blafcraft.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.ClientCooldowns;
import net.blafteam.blafcraft.mana.ClientManaHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
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
        LocalPlayer player = mc.player;
        if (player == null) return;

        // В 1.21 GuiGraphics получаем так:
        GuiGraphics graphics = event.getGuiGraphics();

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        int x = 20;
        int y = 20;

        // Cooldowns
        for (ActionType action : ActionType.values()) {

            int remain = ClientCooldowns.getRemaining(action);

            if (remain > 0) {
                String text = action.name() + ": " + String.format("%.1f", remain / 20.0);
                graphics.drawString(mc.font, text, x + 16, y, 0xFFFFFF);
                y += 10; // сдвиг чтоб не было наслоения
            }
        }

        // Bloodlust Overlay
        if (player.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            ResourceLocation bloodlustOverlay = ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "textures/gui/bloodlust_overlay.png");
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(bloodlustOverlay, 0, 0, 0, 0, w, h, w, h);
            RenderSystem.disableBlend();
        }

        float mana = ClientManaHandler.getMana();
        int maxMana = ClientManaHandler.getMaxMana();

        // Рисуем полоску маны в левом нижнем углу (например)
        int barWidth = 100;
        int barHeight = 10;
        int x1 = 10;
        int y1 = h - 30;

        // Фон
        graphics.fill(x1, y1, x1 + barWidth, y1 + barHeight, 0xFF333333);
        // Заполнение (синий цвет)
        int filledWidth = (int) (barWidth * (mana / maxMana));
        graphics.fill(x1, y1, x1 + filledWidth, y1 + barHeight, 0xFF0000FF);
        // Текст "Мана: 75/100"
        String manaText = "Mana: " + (int) mana + "/" + maxMana;
        graphics.drawString(mc.font, manaText, x1 + barWidth + 5, y1 + 1, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        ClientCooldowns.tick();
    }
}

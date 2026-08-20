package net.blafteam.blafcraft.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RuneScreen extends AbstractContainerScreen<RuneMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "textures/gui/rune_block/rune_block_gui.png");

    public RuneScreen(RuneMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Вертикальная полоска энергии снизу вверх
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        int barWidth = 8;      // ширина
        int barHeight = 50;    // высота
        int barX = x + 140;     // левый верхний угол
        int barY = y + 20;

        // Фон (пустой столбик)
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

        // Заполненная часть снизу вверх
        int filledHeight = maxEnergy > 0 ? (int)((float)energy / maxEnergy * barHeight) : 0;
        if (filledHeight > 0) {
            // Заливаем нижнюю часть: от (barY + barHeight - filledHeight) до (barY + barHeight)
            guiGraphics.fill(barX, barY + barHeight - filledHeight, barX + barWidth, barY + barHeight, 0xFFD97632);
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
package net.blafteam.blafcraft.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blafteam.blafcraft.block.entity.RealizerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class RealizerBlockEntityRenderer implements BlockEntityRenderer<RealizerBlockEntity> {
    public RealizerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

    }
    @Override
    public void render(RealizerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack,
                       MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            ItemStack stack = pBlockEntity.inventory.getStackInSlot(0);

            pPoseStack.pushPose();
            pPoseStack.translate(0.5f, 1.15f, 0.5f);
            pPoseStack.scale(0.5f, 0.5f, 0.5f);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(pBlockEntity.getRenderingRotation())); // around positive y

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, getLightLevel(pBlockEntity.getLevel(),
                    pBlockEntity.getBlockPos()), OverlayTexture.NO_OVERLAY, pPoseStack, pBufferSource, pBlockEntity.getLevel(), 1);
            pPoseStack.popPose(); // render
        }

        private int getLightLevel(Level level, BlockPos pos) {
            int bLight = level.getBrightness(LightLayer.BLOCK, pos);
            int sLight = level.getBrightness(LightLayer.SKY, pos);
            return LightTexture.pack(bLight, sLight);
        }
}

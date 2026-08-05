package net.blafteam.blafcraft.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HighlightRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();
        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();

        for (Integer id : ClientHighlightHandler.highlightedEntityIds) {
            Entity entity = level.getEntity(id);
            if (entity == null) continue;

            // Интерполированные координаты сущности
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

            // Сохраняем матрицу, сдвигаем к позиции камеры
            poseStack.pushPose();
            poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

            // Цвет обводки (R, G, B, A)
            outlineBuffer.setColor(255, 0, 0, 0); // 255 max

            // Рендерим сущность в буфер обводки
            dispatcher.render(
                    entity,
                    0.0, 0.0, 0.0, // теперь относительно сдвинутой матрицы
                    yRot,
                    partialTick,
                    poseStack,
                    outlineBuffer,
                    15728880
            );

            poseStack.popPose(); // восстанавливаем матрицу
        }

        // Завершаем пакет обводок для применения шейдера
        outlineBuffer.endOutlineBatch();
    }

    // PacketDistributor.sendToPlayer((ServerPlayer) player, new HighlightEntityPacket(targetEntity.getId(), true)); - highlight
    // PacketDistributor.sendToPlayer((ServerPlayer) player, new HighlightEntityPacket(targetEntity.getId(), false)); - off highlight
}
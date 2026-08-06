package net.blafteam.blafcraft.highlight;

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

        for (Integer id : ClientHighlightHandler.highlightedEntities.keySet()) {
            Entity entity = level.getEntity(id);
            if (entity == null) continue;

            float[] color = ClientHighlightHandler.getHighlightColor(entity);
            if (color == null) continue;

            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

            poseStack.pushPose();
            poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

            // Преобразуем 0..1 → 0..255
            int red   = (int)(color[0] * 255);
            int green = (int)(color[1] * 255);
            int blue  = (int)(color[2] * 255);
            outlineBuffer.setColor(red, green, blue, 0); // альфа оставляем 0 (полная непрозрачность)

            dispatcher.render(
                    entity,
                    0.0, 0.0, 0.0,
                    yRot,
                    partialTick,
                    poseStack,
                    outlineBuffer,
                    15728880
            );

            poseStack.popPose();
        }

        outlineBuffer.endOutlineBatch();
    }
}

  // Подсветить сущность оранжевым (1.0, 0.5, 0.0)
//PacketDistributor.sendToPlayer((ServerPlayer) player, new HighlightEntityPacket(targetEntity.getId(), true, 1.0f, 0.5f, 0.0f));
//
  // Убрать подсветку
//PacketDistributor.sendToPlayer((ServerPlayer) player, new HighlightEntityPacket(targetEntity.getId(), false, 0f, 0f, 0f));
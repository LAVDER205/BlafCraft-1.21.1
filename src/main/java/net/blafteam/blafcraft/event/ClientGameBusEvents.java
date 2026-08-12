package net.blafteam.blafcraft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientGameBusEvents {
    private static Zombie cachedZombie;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.MORPH_EFFECT)) {
            event.setCanceled(true);

            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            if (cachedZombie == null) {
                cachedZombie = EntityType.ZOMBIE.create(level);
            }
            if (cachedZombie == null) return;

            // Координаты
            cachedZombie.setPos(player.getX(), player.getY(), player.getZ());
            cachedZombie.xOld = player.xOld;
            cachedZombie.yOld = player.yOld;
            cachedZombie.zOld = player.zOld;

            // Повороты
            cachedZombie.setYRot(player.getYRot());
            cachedZombie.setXRot(player.getXRot());
            cachedZombie.yRotO = player.yRotO;
            cachedZombie.xRotO = player.xRotO;
            cachedZombie.yBodyRot = player.yBodyRot;
            cachedZombie.yBodyRotO = player.yBodyRotO;
            cachedZombie.yHeadRot = player.yBodyRot;
            cachedZombie.yHeadRotO = player.yBodyRotO;

            // Анимация ходьбы
            // Прямо копируем позицию и скорость анимации (без накопления)
            float animSpeed = player.walkAnimation.speed() * 0.45F; // подберите множитель под себя
            cachedZombie.walkAnimation.update(animSpeed, player.walkAnimation.position());
            cachedZombie.walkAnimation.setSpeed(animSpeed);

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffer = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

            dispatcher.render(cachedZombie, 0.0D, 0.0D, 0.0D, cachedZombie.getYRot(), event.getPartialTick(),
                    poseStack, buffer, packedLight);
        }
    }
}
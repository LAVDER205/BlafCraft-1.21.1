package net.blafteam.blafcraft.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.morph.ClientMorphData;
import net.blafteam.blafcraft.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientGameBusEvents {

    private static final Map<EntityType<?>, Entity> CACHED_ENTITIES = new HashMap<>();

    //-------------------------------- DISABLING NAMETAGS LOGIC -------------------------------
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        // Скрываем ники только для игроков (включая самих себя)
        if (event.getEntity() instanceof Player) {
            event.setCanRender(TriState.FALSE);
        }
    }

    //-------------------------------- MORPH LOGIC -------------------------------
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.MORPH_EFFECT) && Objects.requireNonNull(player.getEffect(ModEffects.MORPH_EFFECT)).getAmplifier() == 1) {
            ResourceLocation morphId = ClientMorphData.getMorphId();
        if (morphId == null) return;

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Entity cached = CACHED_ENTITIES.get(entityType);
        if (cached == null) {
            cached = entityType.create(level);
            if (cached == null) return;
            CACHED_ENTITIES.put(entityType, cached);
        }

        // Общая информация для любой сущности
        cached.setPos(player.getX(), player.getY(), player.getZ());
        cached.xOld = player.xOld;
        cached.yOld = player.yOld;
        cached.zOld = player.zOld;

        cached.setYRot(player.getYRot());
        cached.setXRot(player.getXRot());
        cached.yRotO = player.yRotO;
        cached.xRotO = player.xRotO;

        // Для живых существ добавляем повороты тела и анимацию
        if (cached instanceof LivingEntity livingCached) {
            livingCached.yBodyRot = player.yBodyRot;
            livingCached.yBodyRotO = player.yBodyRotO;
            livingCached.yHeadRot = player.yBodyRot; // голова следует за телом, не за камерой
            livingCached.yHeadRotO = player.yBodyRotO;

            Vec3 motion = player.getDeltaMovement();
            boolean moving = Math.sqrt(motion.x * motion.x + motion.z * motion.z) > 0.01;
            float animSpeed = moving ? 0.2F : 0.0F; // постоянная скорость при ходьбе

            livingCached.walkAnimation.update(animSpeed, player.walkAnimation.position());
            livingCached.walkAnimation.setSpeed(animSpeed);
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        dispatcher.render(cached, 0.0D, 0.0D, 0.0D, cached.getYRot(), event.getPartialTick(),
                poseStack, buffer, packedLight);
        }
    }
}
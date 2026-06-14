package net.blafteam.blafcraft.keybinds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerHandler {
    public static void handle(ActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player()     instanceof ServerPlayer player) {
                switch (packet.action()) {
                    case SPEED -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                    case JUMP -> player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));
                    case ARROW -> {
                        ThrownPotion arrow = EntityType.POTION.create(player.level());
                        if (arrow == null) return;

                        arrow.setOwner(player);
                        arrow.setPos(player.getEyePosition());

                        Vec3 look = player.getViewVector(1.0F);
                        double speed = 3.0;
                        arrow.shoot(look.x, look.y, look.z, (float) speed, 0.0F);

                        player.level().addFreshEntity(arrow);
                    }
                    };
                }

        });
    }
}

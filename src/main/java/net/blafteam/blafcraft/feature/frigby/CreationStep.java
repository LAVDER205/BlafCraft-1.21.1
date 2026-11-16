package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class CreationStep {
    private CreationStep() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) return;
        if (player instanceof ServerPlayer serverPlayer && !FrigbyController.isFrigby(serverPlayer)) {
            player.removeEffect(ModEffects.CREATION_STEP_EFFECT);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying() || player.isInWaterOrBubble()
                || player.isInLava() || player.onClimbable()) return;

        // Сама способность может выставить onGround и в воздухе.
        // Проверяем настоящую опору под ногами, включая плиты и ступеньки.
        var feet = player.getBoundingBox().move(0, -0.02, 0);
        if (player.level().getBlockCollisions(player, feet).iterator().hasNext()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            int cost = FrigbyBalance.STEP_XP_PER_TICK;
            if (!ExperiencePoints.canAfford(serverPlayer, cost)) {
                player.removeEffect(ModEffects.CREATION_STEP_EFFECT);
                player.displayClientMessage(Component.literal("Опыт закончился"), true);
                return;
            }
            serverPlayer.giveExperiencePoints(-cost);
            FrigbyActivity.mark(serverPlayer);
            spawnHoverCubeParticles(serverPlayer);
        } else if (player.level().isClientSide && !player.onGround()) {
            // Клиент управляет движением, сервер проверяет ресурс и выключает эффект.
            handleMidAirFlight(player);
        }
        player.fallDistance = 0;
    }

    private static void handleMidAirFlight(Player player) {
        Vec3 delta = player.getDeltaMovement();

        if (delta.y >= 0f) {
            return;
        }

        double yPos = player.getY();
        double fractionalY = yPos - Math.floor(yPos);

        BlockPos posBelow = player.blockPosition().below();
        BlockState stateBelow = player.level().getBlockState(posBelow);
        boolean noSolidBlockBelow = stateBelow.getCollisionShape(player.level(), posBelow).isEmpty();

        if (player.isCrouching()) {
            // Manual Descend
            player.setDeltaMovement(delta.x, -0.15, delta.z);
        } else if (fractionalY > 0.85) {
            // Walked of a ledge
            player.setPos(player.getX(), Math.ceil(yPos) + 0.08, player.getZ());
            setEntityHovering(player, delta);
        } else if (fractionalY > 0.04) {
            // Soft Sink
            player.setDeltaMovement(delta.x, -0.04, delta.z);
        } else if (noSolidBlockBelow && fractionalY > 0.0) {
            // Ground Snap
            player.setDeltaMovement(delta.x, -0.04, delta.z);
        } else {
            // Grid Hover
            setEntityHovering(player, delta);
        }

    }

    private static void setEntityHovering(Player player, Vec3 delta) {
        player.setDeltaMovement(delta.x, 0.0f, delta.z);
        player.setOnGround(true);
    }

    private static void spawnHoverCubeParticles(ServerPlayer player) {

        ServerLevel serverLevel = player.serverLevel();

        double playerX = Math.floor(player.getX());
        double playerY = Math.floor(player.getY() - 1);
        double playerZ = Math.floor(player.getZ());

        for (int i = 0; i < 12; i++) {
            double xPos = playerX;
            double yPos = playerY;
            double zPos = playerZ;

            int edge = player.getRandom().nextInt(12);
            double offset = player.getRandom().nextDouble();

            switch (edge) {
                case 0 -> {
                    xPos += offset;
                }
                case 1 -> {
                    xPos += offset;
                    zPos += 1.0;
                }
                case 2 -> {
                    zPos += offset;
                }
                case 3 -> {
                    zPos += offset;
                    xPos += 1.0;
                }

                case 4 -> {
                    xPos += offset;
                    yPos += 1.0;
                }
                case 5 -> {
                    xPos += offset;
                    zPos += 1.0;
                    yPos += 1.0;
                }
                case 6 -> {
                    zPos += offset;
                    yPos += 1.0;
                }
                case 7 -> {
                    zPos += offset;
                    xPos += 1.0;
                    yPos += 1.0;
                }

                case 8 -> {
                    yPos += offset;
                }
                case 9 -> {
                    yPos += offset;
                    zPos += 1.0;
                }
                case 10 -> {
                    yPos += offset;
                    xPos += 1.0;
                }
                case 11 -> {
                    yPos += offset;
                    xPos += 1.0;
                    zPos += 1.0;
                }
            }

            serverLevel.sendParticles(player, ParticleTypes.END_ROD, false, xPos, yPos, zPos, 1, 0.0, 0.0, 0.0, 0.002f);
        }
    }
}

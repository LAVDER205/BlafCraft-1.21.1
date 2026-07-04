package net.blafteam.blafcraft.event;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.item.custom.HammerItem;
import net.blafteam.blafcraft.potion.ModPotions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    // Done with the help of https://github.com/CoFH/CoFHCore/blob/1.19.x/src/main/java/cofh/core/event/AreaEffectEvents.java
    // Don't be a jerk License
    // --------------------------------HAMMER LOGIC -------------------------------
    @SubscribeEvent
    public static void onHammerUsage(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer) {
            BlockPos initialBlockPos = event.getPos();
            if(HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return;
            }

            for(BlockPos pos : HammerItem.getBlocksToBeDestroyed(1, initialBlockPos, serverPlayer)) {
                if(pos == initialBlockPos || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
                    continue; // skip
                }

                player.getPersistentData().putBoolean("blafcraft:is_hovering", true);

                HARVESTED_BLOCKS.add(pos);
                serverPlayer.gameMode.destroyBlock(pos);
                HARVESTED_BLOCKS.remove(pos);
            }
        }
    }

    // --------------------------------BREWING RECIPES-------------------------------

    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, Items.SLIME_BALL, ModPotions.SLIMEY_POTION);
    }

    // --------------------------------SCULK SWORD LOGIC -------------------------------
    @SubscribeEvent
    public static void onSculkSwordHit(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() == ModItems.SCULK_SWORD.get() && player instanceof ServerPlayer serverPlayer) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 10, 0));
            }
        }
        if(mainHandItem.getItem() == ModItems.SCULK_AXE.get() && player instanceof ServerPlayer serverPlayer) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 0));
            }
        }
    }

    // --------------------------------CREATION STEP LOGIC -------------------------------
    @SubscribeEvent
    public static void onPlayerTick_CreationStep(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        if (player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) {
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) { spawnHoverCubeParticles(player, serverPlayer); }
            handleAirWalking(player);
        }
    }

    private static void handleAirWalking(Player player) {
        player.fallDistance = 0f;
        if (player.level().isClientSide && !player.onGround()) {
            handleMidAirFlight(player);
        }
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
        boolean hasSolidGroundBelow = stateBelow.getCollisionShape(player.level(), posBelow).isEmpty(); // torch or grass included

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
        } else if (hasSolidGroundBelow && fractionalY > 0.0) {
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

    public static void spawnHoverCubeParticles(Player player, ServerPlayer serverPlayer) {

        ServerLevel serverLevel = (ServerLevel) player.level();

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
                case 0 -> { xPos += offset; }
                case 1 -> { xPos += offset; zPos += 1.0; }
                case 2 -> { zPos += offset; }
                case 3 -> { zPos += offset; xPos += 1.0; }

                case 4 -> { xPos += offset; yPos += 1.0; }
                case 5 -> { xPos += offset; zPos += 1.0; yPos += 1.0; }
                case 6 -> { zPos += offset; yPos += 1.0; }
                case 7 -> { zPos += offset; xPos += 1.0; yPos += 1.0; }

                case 8 -> { yPos += offset; }
                case 9 -> { yPos += offset; zPos += 1.0; }
                case 10 -> { yPos += offset; xPos += 1.0; }
                case 11 -> { yPos += offset; xPos += 1.0; zPos += 1.0; }
            }

            serverLevel.sendParticles(serverPlayer, ParticleTypes.END_ROD, false, xPos, yPos, zPos, 1, 0.0, 0.0, 0.0, 0.002f);
        }
    }

    // -------------------------------- BLOODLUST LOGIC -------------------------------

    @SubscribeEvent
    public static void onHitWithBloodlust(AttackEntityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            livingEntity.heal(2.0F);
        }
    }

    @SubscribeEvent
    public static void onCriticalHitWithBloodlust(CriticalHitEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            livingEntity.heal(4.0F);
        }
    }

    @SubscribeEvent
    public static void onKillWithBloodlust(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (event.getSource().getEntity() instanceof LivingEntity killerEntity && killerEntity.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            if (deadEntity instanceof Player) killerEntity.heal(20.0f);
                    else killerEntity.heal(10.0f);
        }
    }

    // -------------------------------- MILK LOGIC -------------------------------
//    @SubscribeEvent
//    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
//        Player player = event.getEntity();
//        ItemStack stack = event.getItemStack();
//
//        if (stack.is(Items.MILK_BUCKET) && player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) {
//            event.setCanceled(true);
//        }
//    }
}

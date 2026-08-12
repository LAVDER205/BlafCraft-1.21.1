package net.blafteam.blafcraft.event;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.highlight.HighlightEntityPacket;
import net.blafteam.blafcraft.highlight.HighlightManager;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.item.custom.HammerItem;
import net.blafteam.blafcraft.keybinds.ServerHandler;
import net.blafteam.blafcraft.potion.ModPotions;
import net.blafteam.blafcraft.sound.LoopingSoundPayload;
import net.blafteam.blafcraft.sound.ModSounds;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

        if (mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer) {
            BlockPos initialBlockPos = event.getPos();
            if (HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return;
            }

            for (BlockPos pos : HammerItem.getBlocksToBeDestroyed(1, initialBlockPos, serverPlayer)) {
                if (pos == initialBlockPos || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
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

        createStandardPotions(builder, Items.FIRE_CHARGE, ModPotions.FIERY_TOUCH_POTION, ModPotions.STRONG_FIERY_TOUCH_POTION, ModPotions.LONG_FIERY_TOUCH_POTION);
        createStandardPotions(builder, Items.IRON_NUGGET, ModPotions.RESISTANCE_POTION, ModPotions.STRONG_RESISTANCE_POTION, ModPotions.LONG_RESISTANCE_TOUCH_POTION);
        createStandardPotions(builder, Items.WIND_CHARGE, ModPotions.QUICK_ATTACK_POTION, ModPotions.STRONG_QUICK_ATTACK_POTION, ModPotions.LONG_QUICK_ATTACK_POTION);
    }

    private static void createStandardPotions(PotionBrewing.Builder builder, Item item,
                                              Holder <Potion> normal_p,
                                              Holder <Potion> strong_p,
                                              Holder <Potion> long_p) {
        builder.addMix(Potions.AWKWARD, item, normal_p);
        builder.addMix(ModPotions.FIERY_TOUCH_POTION, Items.GLOWSTONE_DUST, strong_p);
        builder.addMix(ModPotions.FIERY_TOUCH_POTION, Items.REDSTONE, long_p);
    }

    // --------------------------------SCULK SWORD LOGIC -------------------------------
    @SubscribeEvent
    public static void onSculkSwordHit(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.getItem() == ModItems.SCULK_SWORD.get() && player instanceof ServerPlayer serverPlayer) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 10, 0));
            }
        }
        if (mainHandItem.getItem() == ModItems.SCULK_AXE.get() && player instanceof ServerPlayer serverPlayer) {
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
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                spawnHoverCubeParticles(player, serverPlayer);
            }
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

            serverLevel.sendParticles(serverPlayer, ParticleTypes.END_ROD, false, xPos, yPos, zPos, 1, 0.0, 0.0, 0.0, 0.002f);
        }
    }

    // -------------------------------- BLOODLUST LOGIC -------------------------------

    @SubscribeEvent
    public static void onHitWithBloodlust(AttackEntityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity targetEntity = event.getTarget();
        if (targetEntity instanceof LivingEntity && livingEntity.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            livingEntity.heal(2.0F);
            if (targetEntity.getRandom().nextFloat() < 0.5f) { // 50%
                ((LivingEntity) targetEntity).addEffect(new MobEffectInstance(ModEffects.BLEEDING_EFFECT, 100, 0, false, false, true));
            }

        }
    }

    @SubscribeEvent
    public static void onCriticalHitWithBloodlust(CriticalHitEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity targetEntity = event.getTarget();
        if (targetEntity instanceof LivingEntity && livingEntity.hasEffect(ModEffects.BLOODLUST_EFFECT)) {
            livingEntity.heal(4.0F);
            ((LivingEntity) targetEntity).addEffect(new MobEffectInstance(ModEffects.BLEEDING_EFFECT, 100, 0, false, false, true));
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

    private static final ResourceLocation BLOODLUST_ID =
            ResourceLocation.fromNamespaceAndPath("blafcraft", "bloodlust");

    @SubscribeEvent
    public static void onBloodlustEffectRemove(MobEffectEvent.Remove event) {
        Holder<MobEffect> holder = event.getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(BLOODLUST_ID)) {
            EffectCure effectCure = event.getCure();
            if (effectCure != null && effectCure.equals(EffectCures.MILK)) {
                event.setCanceled(true);
            } else if (event.getEntity() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new LoopingSoundPayload(ModSounds.HEARTBEAT.get(), 1.4f, 1.0f, false));
            }
        }
    }

    @SubscribeEvent
    public static void onBloodlustEffectExpired(MobEffectEvent.Expired event) {
        Holder<MobEffect> holder = event.getEffectInstance().getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(BLOODLUST_ID)) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new LoopingSoundPayload(ModSounds.HEARTBEAT.get(), 1.4f, 1.0f, false));
            }
        }
    }

    // -------------------------------- MILK LOGIC -------------------------------
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.is(Items.MILK_BUCKET) && (player.hasEffect(ModEffects.OVERDOSE_EFFECT) || player.hasEffect(ModEffects.POTION_SICKNESS_EFFECT))) {
            event.setCanceled(true);
        }
    }

    // -------------------------------- OVERDOSE & POTION SICKNESS LOGIC -------------------------------

    private static final ResourceLocation OVERDOSE_ID =
            ResourceLocation.fromNamespaceAndPath("blafcraft", "overdose");

    private static final ResourceLocation POTION_SICKNESS_ID =
            ResourceLocation.fromNamespaceAndPath("blafcraft", "potion_sickness");

    @SubscribeEvent
    public static void onOverdoseEffectExpired(MobEffectEvent.Expired event) {
        Holder<MobEffect> holder = event.getEffectInstance().getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(OVERDOSE_ID)) {
            event.getEntity().addEffect(new MobEffectInstance(ModEffects.POTION_SICKNESS_EFFECT, 3600, 0, true, true, true));
        }
    }

    @SubscribeEvent
    public static void onPotionSicknessDamaged(LivingDamageEvent.Pre event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.hasEffect(ModEffects.POTION_SICKNESS_EFFECT)) {
            float currentDamage = event.getNewDamage();
            event.setNewDamage(currentDamage * 1.5f);
        }
    }

    @SubscribeEvent
    public static void onPotionSicknessRemovedWithMilk(MobEffectEvent.Remove event) {
        Holder<MobEffect> holder = event.getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(POTION_SICKNESS_ID)) {
            assert event.getCure() != null;
            EffectCure effectCure = event.getCure();
            if (effectCure != null && effectCure.equals(EffectCures.MILK)) {
                event.setCanceled(true);
            }
        }
    }

    // -------------------------------- LAVDER PASSIVE LOGIC -------------------------------
    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (stack.is(Items.POTION) && player.getMainHandItem().getItem() != ModItems.STIMULATOR.get() && event.getEntity().getName().getString().equals("Dev")) {
            PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            for (MobEffectInstance effect : contents.getAllEffects()) {
                event.getEntity().addEffect(new MobEffectInstance(effect));
            }

            player.setItemInHand(event.getHand(), new ItemStack(Items.GLASS_BOTTLE));
        }
    }

    // -------------------------------- FIERY TOUCH LOGIC -------------------------------
    @SubscribeEvent
    public static void onHitWithFieryTouch(AttackEntityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity targetEntity = event.getTarget();
        if (targetEntity instanceof LivingEntity && livingEntity.hasEffect(ModEffects.FIERY_TOUCH_EFFECT)) {
            int amplifier = Objects.requireNonNull(livingEntity.getEffect(ModEffects.FIERY_TOUCH_EFFECT)).getAmplifier();
            targetEntity.igniteForTicks(60 + (20 * amplifier));
        }
    }

    // -------------------------------- TIME BOMB LOGIC -------------------------------
    private static final ResourceLocation TIME_BOMB_ID =
            ResourceLocation.fromNamespaceAndPath("blafcraft", "time_bomb");

    @SubscribeEvent
    public static void onTimeBombRemovedWithMilk(MobEffectEvent.Remove event) {
        Holder<MobEffect> holder = event.getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(TIME_BOMB_ID)) {
            EffectCure effectCure = event.getCure();
            if (effectCure != null && effectCure.equals(EffectCures.MILK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTimeBombEffectExpired(MobEffectEvent.Expired event) {
        Holder<MobEffect> holder = event.getEffectInstance().getEffect();
        int amplifier = event.getEffectInstance().getAmplifier();
        Entity entity = event.getEntity();

        if (holder.getKey() != null && holder.getKey().location().equals(TIME_BOMB_ID)) {
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                if (amplifier == 1)
                    serverLevel.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 10.0f, false, Level.ExplosionInteraction.TRIGGER);
                else if (amplifier == 0) {
                    serverLevel.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 1.5f, false, Level.ExplosionInteraction.TRIGGER);
                    entity.hurt(entity.damageSources().explosion(null), 6);
                    launchUp(entity, 1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHitWithTimeBomb(AttackEntityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity targetEntity = event.getTarget();

        if (!livingEntity.level().isClientSide) {
            if (targetEntity instanceof LivingEntity && livingEntity.hasEffect(ModEffects.TIME_BOMB_EFFECT) && livingEntity.getEffect(ModEffects.TIME_BOMB_EFFECT).getAmplifier() == 1) {
                int duration = Objects.requireNonNull(livingEntity.getEffect(ModEffects.TIME_BOMB_EFFECT)).getDuration();
                livingEntity.removeEffect(ModEffects.TIME_BOMB_EFFECT);
                ((LivingEntity) targetEntity).addEffect(new MobEffectInstance(ModEffects.TIME_BOMB_EFFECT, duration, 0, false, true, true));
            }
        }
    }

    public static void launchUp(Entity entity, double strength) {
        if (entity.level().isClientSide) return;


        entity.setPos(entity.getX(), entity.getY() + 0.5, entity.getZ());
        entity.setOnGround(false);
        entity.hasImpulse = true;


        entity.setDeltaMovement(entity.getDeltaMovement().x, strength, entity.getDeltaMovement().z);


        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        } else if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(entity, new ClientboundSetEntityMotionPacket(entity));
        }
    }

    // -------------------------------- SCULK INFECTION LOGIC -------------------------------
    private static final ResourceLocation SCULK_INFECTION_ID =
            ResourceLocation.fromNamespaceAndPath("blafcraft", "sculk_infection");

    private static final double DETECTION_RADIUS = 25.0;

    @SubscribeEvent
    public static void onSculkInfectionRemoved(MobEffectEvent.Remove event) {
        // MILK
        Holder<MobEffect> holder = event.getEffect();
        if (holder.getKey() != null && holder.getKey().location().equals(SCULK_INFECTION_ID)) {
            EffectCure effectCure = event.getCure();
            if (effectCure != null && effectCure.equals(EffectCures.MILK)) {
                event.setCanceled(true);
            }
            // OTHER
            if (Objects.requireNonNull(event.getEntity().getEffect(ModEffects.SCULK_INFECTION_EFFECT)).getAmplifier() == 1) {
                event.getEntity().addEffect(new MobEffectInstance(ModEffects.SCULK_INFECTION_EFFECT, -1, 0, false, false, false));
            }
        }
    }

    @SubscribeEvent
    public static void onSculkInfectionEffectExpired(MobEffectEvent.Expired event) {
        assert event.getEffectInstance() != null;
        Holder<MobEffect> holder = event.getEffectInstance().getEffect();
        int amplifier = event.getEffectInstance().getAmplifier();
        Entity entity = event.getEntity();

        if (holder.getKey() != null && holder.getKey().location().equals(SCULK_INFECTION_ID)) {
            if (Objects.requireNonNull(event.getEntity().getEffect(ModEffects.SCULK_INFECTION_EFFECT)).getAmplifier() == 1) {
                event.getEntity().addEffect(new MobEffectInstance(ModEffects.SCULK_INFECTION_EFFECT, -1, 0, false, false, false));
            }
        }
    }

    @SubscribeEvent
    public static void onSoundPlayed(PlaySoundEvent event) {
        if (event.getSound() != null) {
            if (event.getSound().getSource() != SoundSource.HOSTILE
                    && event.getSound().getSource() != SoundSource.NEUTRAL
                    && event.getSound().getSource() != SoundSource.PLAYERS) {
                return;
            }

            Entity source = findEntityAtSoundPosition(event.getSound());
            if (!(source instanceof LivingEntity living)) return;

            ServerLevel level = (ServerLevel) living.level();
            double radius = 50.0;
            Vec3 pos = living.position();
            AABB area = new AABB(pos.subtract(radius, radius, radius), pos.add(radius, radius, radius));

            List<ServerPlayer> affectedPlayers = level.getEntitiesOfClass(ServerPlayer.class, area,
                    p -> p.hasEffect(ModEffects.SCULK_INFECTION_EFFECT) && !p.equals(living));

            for (ServerPlayer player : affectedPlayers) {
                if (!HighlightManager.isHighlighted(player, living)) {
                    HighlightManager.highlight(player, living, 1.0f, 1.0f, 1.0f);
                    HighlightManager.scheduleUnhighlightWithUpdate(player, living, 20);
                }
            }
        }
    }

    private static Entity findEntityAtSoundPosition(SoundInstance sound) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();

        // Ищем во всех загруженных мирах
        for (ServerLevel level : server.getAllLevels()) {
            // Получаем всех сущностей в маленьком кубике вокруг звука
            List<Entity> candidates = level.getEntitiesOfClass(Entity.class,
                    new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5));
            for (Entity e : candidates) {
                // Точная проверка (расстояние меньше 0.1 блока)
                if (e.position().distanceToSqr(x, y, z) < 0.01) {
                    return e;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (level == null) return;

        for (ServerPlayer player : level.players()) {
            if (!player.hasEffect(ModEffects.SCULK_INFECTION_EFFECT)) continue;

            // --- 1. Движущиеся игроки (кроме крадущихся) ---
            List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(player.position().subtract(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS),
                            player.position().add(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)),
                    other -> !other.equals(player)
                            && other.getDeltaMovement().lengthSqr() > 0.0001
                            && !other.isCrouching()
            );

            for (ServerPlayer noisy : nearbyPlayers) {
                if (!HighlightManager.isHighlighted(player, noisy)) {
                    HighlightManager.highlight(player, noisy, 1.0f, 1.0f, 1.0f);
                    HighlightManager.scheduleUnhighlightWithUpdate(player, noisy, 20);
                }
            }

            // --- 2. Движущиеся мобы (только горизонтальное перемещение) ---
            List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class,
                    new AABB(player.position().subtract(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS),
                            player.position().add(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)),
                    mob -> {
                        Vec3 motion = mob.getDeltaMovement();
                        // Учитываем только горизонтальную скорость, игнорируем Y (гравитацию)
                        double horizSqr = motion.x * motion.x + motion.z * motion.z;
                        return horizSqr > 0.001; // порог чуть выше, чем 0.0001
                    }
            );

            for (Mob mob : nearbyMobs) {
                if (!HighlightManager.isHighlighted(player, mob)) {
                    HighlightManager.highlight(player, mob, 1.0f, 1.0f, 1.0f);
                    HighlightManager.scheduleUnhighlightWithUpdate(player, mob, 20);
                }
            }
        }
    }

    // -------------------------------- FREE FLIGHT LOGIC -------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.FREE_FLIGHT_EFFECT)) {
            return;
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
    }

    // -------------------------------- SCULK MARK LOGIC -------------------------------
    @SubscribeEvent
    public static void onHitWithSculkMark(AttackEntityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity targetEntity = event.getTarget();

        if (!livingEntity.level().isClientSide) {
            if (targetEntity instanceof LivingEntity && livingEntity.hasEffect(ModEffects.SCULK_MARK_EFFECT) && Objects.requireNonNull(livingEntity.getEffect(ModEffects.SCULK_MARK_EFFECT)).getAmplifier() == 1) {
                livingEntity.removeEffect(ModEffects.SCULK_MARK_EFFECT);
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false, true));
                livingEntity.addEffect(new MobEffectInstance(ModEffects.SCULK_INFECTION_EFFECT, 200, 1, false, false, true));
                ((LivingEntity) targetEntity).addEffect(new MobEffectInstance(ModEffects.SCULK_MARK_EFFECT, 200, 0, false, false, true));
                HighlightManager.highlight((ServerPlayer) livingEntity, targetEntity, 1.0f, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onSculkMarkDamaged(LivingDamageEvent.Pre event) {
        LivingEntity targetEntity = event.getEntity();
        if (targetEntity.hasEffect(ModEffects.SCULK_MARK_EFFECT) &&
                Objects.requireNonNull(targetEntity.getEffect(ModEffects.SCULK_MARK_EFFECT)).getAmplifier() == 0 &&
                event.getSource().getEntity() instanceof LivingEntity livingEntity &&
                livingEntity.hasEffect(ModEffects.SCULK_INFECTION_EFFECT)) {
            float currentDamage = event.getNewDamage();
            event.setNewDamage(currentDamage * 1.5f);
        }
    }
}

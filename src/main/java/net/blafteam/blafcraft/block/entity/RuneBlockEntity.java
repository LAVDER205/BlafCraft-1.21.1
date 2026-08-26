package net.blafteam.blafcraft.block.entity;

import net.blafteam.blafcraft.entity.custom.LargeArtilleryFireballEntity;
import net.blafteam.blafcraft.entity.custom.SmallArtilleryFireballEntity;
import net.blafteam.blafcraft.friend_system.FriendManager;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.item.custom.RuneItem;
import net.blafteam.blafcraft.screen.custom.RuneMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

public class RuneBlockEntity extends BlockEntity implements MenuProvider, ContainerData {

    private int ShootTickCounter = 0;
    private int ParticleTickCounter = 0;
    private int EffectTickCounter = 0;

    private static final int ARROW_SHOOT_INTERVAL_TICKS = 10;
    private static final int FIREBALL_SHOOT_INTERVAL_TICKS = 20;
    private static final int LARGE_FIREBALL_SHOOT_INTERVAL_TICKS = 40;
    private static final int PARTICLE_INTERVAL_TICKS = 40;
    private static final int EFFECT_INTERVAL_TICKS = 40;

    private static final int ENERGY_COST_ARROW = 5;
    private static final int ENERGY_COST_FIREBALL = 10;
    private static final int ENERGY_COST_LARGE_FIREBALL = 20;

    private static final double ARROW_TARGET_RADIUS = 10.0;
    private static final double FIREBALL_TARGET_RADIUS = 20.0;
    private static final double LARGE_FIREBALL_TARGET_RADIUS = 40.0;

    private static final double EFFECT_RADIUS = 20.0;

    private UUID ownerUUID = null;

    // ===== Энергия =====
    private int energy = 0;
    private final int maxEnergy = 100; // например, максимальная ёмкость

    public final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (slot == 0 && stack.getItem() instanceof RuneItem item && (item.equals(ModItems.SHARP_RUNE.get()) ||
                                                                                item.equals(ModItems.FIRE_RUNE.get()) ||
                                                                                item.equals(ModItems.EXPLOSIVE_RUNE.get())))  ||

                    (slot == 1 && stack.getItem() instanceof RuneItem item2 && (item2.equals(ModItems.SPEED_RUNE.get()) ||
                                                                                item2.equals(ModItems.STRENGTH_RUNE.get()) ||
                                                                                item2.equals(ModItems.HEAL_RUNE.get()) ||
                                                                                item2.equals(ModItems.DEFENCE_RUNE.get()))) ||

                    (slot == 2 && stack.getItem().equals(ModItems.LIQUID_COPPER.get()));
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            switch (slot) {
                case 0, 1 -> {
                    return 1;
                }
                case 2 -> {
                    return 64;
                }
            }
            return 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public RuneBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RUNE_ACTION_BE.get(), pos, blockState);
    }

    // ===== Методы энергии =====
    public int getEnergy() {
        return energy;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(value, maxEnergy));
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void addEnergy(int amount) {
        setEnergy(this.energy + amount);
    }

    public boolean consumeEnergy(int amount) {
        if (this.energy >= amount) {
            this.energy -= amount;
            markEnergyChanged();
            return true;
        }
        ItemStack fuelStack = inventory.getStackInSlot(2);
        if (!fuelStack.isEmpty() && fuelStack.getItem() == ModItems.LIQUID_COPPER.get()) {
            fuelStack.shrink(1);
            this.energy = this.maxEnergy - amount;
            markEnergyChanged();
            return true;
        }
        return false;
    }

    private void markEnergyChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
        setChanged();
    }

    // ===== Тикер =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, RuneBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.ShootTickCounter++;
        blockEntity.ParticleTickCounter++;
        blockEntity.EffectTickCounter++;

        Item attackRune = blockEntity.inventory.getStackInSlot(0).getItem();
        Item effectRune = blockEntity.inventory.getStackInSlot(1).getItem();

        // Пополнение энергии, если она на нуле и есть топливо
        if (blockEntity.energy <= 0) {
            ItemStack fuelStack = blockEntity.inventory.getStackInSlot(2);
            if (!fuelStack.isEmpty() && fuelStack.getItem() == ModItems.LIQUID_COPPER.get()) {
                fuelStack.shrink(1);
                blockEntity.energy = blockEntity.maxEnergy;
                blockEntity.setChanged();
            }
        }

        // Частицы радиуса
        if (blockEntity.ParticleTickCounter >= PARTICLE_INTERVAL_TICKS) {
            blockEntity.ParticleTickCounter = 0;
            if (!blockEntity.inventory.getStackInSlot(0).isEmpty()) {
                if (attackRune == ModItems.SHARP_RUNE.get()) {
                    blockEntity.spawnRadiusParticles((ServerLevel) level, ARROW_TARGET_RADIUS);
                } else if (attackRune == ModItems.FIRE_RUNE.get()) {
                    blockEntity.spawnRadiusParticles((ServerLevel) level, FIREBALL_TARGET_RADIUS);
                } else if (attackRune == ModItems.EXPLOSIVE_RUNE.get()) {
                    blockEntity.spawnRadiusParticles((ServerLevel) level, LARGE_FIREBALL_TARGET_RADIUS);
                }
            }
            if (!blockEntity.inventory.getStackInSlot(1).isEmpty()) {
                blockEntity.spawnRadiusParticles((ServerLevel) level, EFFECT_RADIUS);
            }
        }

        // Эффекты для союзников с нагрудником REACTOR
        if (blockEntity.EffectTickCounter >= EFFECT_INTERVAL_TICKS) {
            blockEntity.EffectTickCounter = 0;
            applyEffectToNearbyPlayers((ServerLevel) level, pos, blockEntity.inventory);
        }

        // Стрельба в зависимости от атакующей руны
        if (attackRune == ModItems.SHARP_RUNE.get() && blockEntity.ShootTickCounter >= ARROW_SHOOT_INTERVAL_TICKS) {
            blockEntity.ShootTickCounter = 0;
            if (blockEntity.shootArrowAtNearestTarget()) {
                blockEntity.consumeEnergy(ENERGY_COST_ARROW);
            }
        } else if (attackRune == ModItems.FIRE_RUNE.get() && blockEntity.ShootTickCounter >= FIREBALL_SHOOT_INTERVAL_TICKS) {
            blockEntity.ShootTickCounter = 0;
            if (blockEntity.shootSmallFireballAtNearestTarget()) {
                blockEntity.consumeEnergy(ENERGY_COST_FIREBALL);
            }
        } else if (attackRune == ModItems.EXPLOSIVE_RUNE.get() && blockEntity.ShootTickCounter >= LARGE_FIREBALL_SHOOT_INTERVAL_TICKS) {
            blockEntity.ShootTickCounter = 0;
            if (blockEntity.shootLargeFireballAtNearestTarget()) {
                blockEntity.consumeEnergy(ENERGY_COST_LARGE_FIREBALL);
            }
        }

        blockEntity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private boolean shootArrowAtNearestTarget() {
        if (this.level == null) return false;
        BlockPos pos = this.getBlockPos();
        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
        LivingEntity target = findNearestTarget(pos, startVec, ARROW_TARGET_RADIUS);
        if (target == null) return false;

        Arrow arrow = EntityType.ARROW.create(this.level);
        if (arrow == null) return false;
        arrow.setPos(startVec);
        Vec3 targetVec = target.getEyePosition(1.0F);
        Vec3 direction = targetVec.subtract(startVec).normalize();
        arrow.shoot(direction.x, direction.y, direction.z, 1.5f, 2.0f);
        this.level.addFreshEntity(arrow);
        return true;
    }

    private boolean shootSmallFireballAtNearestTarget() {
        if (this.level == null) return false;
        BlockPos pos = this.getBlockPos();
        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        LivingEntity target = findNearestTarget(pos, startVec, FIREBALL_TARGET_RADIUS);
        if (target == null) return false;

        SmallArtilleryFireballEntity fireball = new SmallArtilleryFireballEntity(
                this.level, target, startVec.x, startVec.y, startVec.z);
        this.level.addFreshEntity(fireball);
        return true;
    }

    private boolean shootLargeFireballAtNearestTarget() {
        if (this.level == null) return false;
        BlockPos pos = this.getBlockPos();
        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
        LivingEntity target = findNearestTarget(pos, startVec, LARGE_FIREBALL_TARGET_RADIUS);
        if (target == null) return false;

        LargeArtilleryFireballEntity fireball = new LargeArtilleryFireballEntity(
                this.level, target, startVec.x, startVec.y, startVec.z);
        this.level.addFreshEntity(fireball);
        return true;
    }

    private static void applyEffectToNearbyPlayers(ServerLevel level, BlockPos pos, ItemStackHandler inventory) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        Vec3 center = pos.getCenter();
        Item rune = inventory.getStackInSlot(1).getItem();

        Holder<MobEffect> effect = null;
        if (rune == ModItems.SPEED_RUNE.get()) effect = MobEffects.MOVEMENT_SPEED;
        else if (rune == ModItems.STRENGTH_RUNE.get()) effect = MobEffects.DAMAGE_BOOST;
        else if (rune == ModItems.HEAL_RUNE.get()) effect = MobEffects.REGENERATION;
        else if (rune == ModItems.DEFENCE_RUNE.get()) effect = MobEffects.DAMAGE_RESISTANCE;

        if (effect == null) return;

        for (ServerPlayer sp : level.players()) {
            if (sp.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.REACTOR) &&
                    sp.position().distanceToSqr(center) <= radiusSq) {
                sp.addEffect(new MobEffectInstance(effect, 60, 1, true, false, true));
            }
        }
    }

    private LivingEntity findNearestTarget(BlockPos pos, Vec3 startVec, double radius) {
        AABB searchArea = new AABB(pos).inflate(radius);
        return this.level.getEntitiesOfClass(LivingEntity.class, searchArea,
                        e -> e.isAlive() && isValidTarget(e))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(startVec)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ServerPlayer owner = getOwner();
            if (owner != null) {
                // Исключаем владельца
                if (player.getUUID().equals(ownerUUID)) {
                    return false;
                }
                // Проверяем, является ли player другом владельца
                if (FriendManager.isFriend(owner, player)) {
                    return false;
                }
            }
            // Если владелец офлайн или дружба не определена, решаем:
            // например, атаковать всех (кроме владельца, если он онлайн)
            // Можно вернуть false, чтобы не стрелять по игрокам без владельца
            return owner != null; // атакуем игроков только если владелец онлайн
        }
        return true; // мобов всегда атакуем
    }

    private void spawnRadiusParticles(ServerLevel level, double radius) {
        BlockPos pos = this.getBlockPos();
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double y = pos.getY() + 1.2;
        int particleCount = 24;

        // Перебираем всех игроков на сервере, находящихся в разумном радиусе
        for (ServerPlayer player : level.players()) {
            // Проверяем, надет ли железный шлем (любой слот, но обычно голова)
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.is(ModItems.OCULAR_OF_OTHER_SIGHT)) continue; // пропускаем без шлема

            // Отправляем частицы этому игроку
            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = centerX + radius * Math.cos(angle);
                double z = centerZ + radius * Math.sin(angle);
                level.sendParticles(player, ParticleTypes.END_ROD, true, x, y, z, 1, 0, 0, 0, 0.01);
            }
        }
    }

    @Nullable
    private ServerPlayer getOwner() {
        if (ownerUUID == null || !(this.level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
    }

    // ===== ContainerData =====
    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> this.energy;
            case 1 -> this.maxEnergy;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        if (index == 0) {
            energy = value;
        }
    }

    @Override
    public int getCount() {
        return 2; // обязательно возвращает 2
    }

    // Other methods

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, ItemStack.EMPTY);
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putInt("Energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        else ownerUUID = null;
        energy = tag.getInt("Energy");
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Rune Action Block");
    }

    @Override
    public @org.jetbrains.annotations.Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RuneMenu(containerId, playerInventory, this);
    }

    @javax.annotation.Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}

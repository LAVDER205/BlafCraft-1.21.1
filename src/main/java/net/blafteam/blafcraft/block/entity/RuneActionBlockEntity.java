package net.blafteam.blafcraft.block.entity;

import net.blafteam.blafcraft.entity.custom.LargeArtilleryFireballEntity;
import net.blafteam.blafcraft.entity.custom.SmallArtilleryFireballEntity;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.screen.custom.RuneActionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Comparator;
import java.util.List;

public class RuneActionBlockEntity extends BlockEntity implements MenuProvider, ContainerData {

    private int tickCounter = 0;
    private static final int SHOOT_INTERVAL_TICKS = 20; // раз в секунду
    private static final int ENERGY_COST = 5;     // энергия за одну стрелу
    private static final double TARGET_RADIUS = 20.0;

    // ===== Энергия =====
    private int energy = 0;
    private final int maxEnergy = 100; // например, максимальная ёмкость

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0  ||
                    (slot == 1 && stack.getItem().equals(ModItems.LIQUID_COPPER.get()));
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            switch (slot) {
                case 0 -> {
                    return 1;
                }
                case 1 -> {
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

    public RuneActionBlockEntity(BlockPos pos, BlockState blockState) {
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
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        } else if (!inventory.getStackInSlot(1).isEmpty()) {
            inventory.getStackInSlot(1).shrink(1);
            this.energy = this.maxEnergy - amount - this.energy;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    // ===== Тикер =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, RuneActionBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.tickCounter++;

        // Попытка пополнить энергию из слота 1, если она на нуле
        if (blockEntity.energy <= 0) {
            ItemStack fuelStack = blockEntity.inventory.getStackInSlot(1);
            if (!fuelStack.isEmpty() && fuelStack.getItem() == ModItems.LIQUID_COPPER.get()) {
                fuelStack.shrink(1);
                blockEntity.energy = blockEntity.maxEnergy;
                blockEntity.setChanged();
            }
        }

        // Стрельба по таймеру, если энергии достаточно
        if (blockEntity.tickCounter >= SHOOT_INTERVAL_TICKS) {
            blockEntity.tickCounter = 0;
            if (blockEntity.energy >= ENERGY_COST) {
                if (blockEntity.shootSmallFireballAtNearestTarget()) {
                    blockEntity.consumeEnergy(ENERGY_COST);
                    blockEntity.setChanged();
                }
            }
        }

        // Обновляем блок (синхронизация с клиентом)
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private boolean shootLargeFireballAtNearestTarget() { // not working for now
        if (this.level == null) return false;
        ServerLevel serverLevel = (ServerLevel) this.level;
        BlockPos pos = this.getBlockPos();

        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
        AABB searchArea = new AABB(pos).inflate(TARGET_RADIUS);
        List<LivingEntity> candidates = this.level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e.isAlive() && !(e instanceof Player));

        if (candidates.isEmpty()) return false;

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(startVec)))
                .orElse(null);
        if (target == null) return false;

        LargeArtilleryFireballEntity fireball = new LargeArtilleryFireballEntity(
                this.level,
                target,
                startVec.x, startVec.y, startVec.z
        );
        this.level.addFreshEntity(fireball);

        return true;
    }

    private boolean shootSmallFireballAtNearestTarget() {
        if (this.level == null) return false;
        ServerLevel serverLevel = (ServerLevel) this.level;
        BlockPos pos = this.getBlockPos();

        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        AABB searchArea = new AABB(pos).inflate(TARGET_RADIUS);
        List<LivingEntity> candidates = this.level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e.isAlive() && !(e instanceof Player)); // исключаем игрока, если нужно

        if (candidates.isEmpty()) return false;

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(startVec)))
                .orElse(null);
        if (target == null) return false;

        SmallArtilleryFireballEntity fireball = new SmallArtilleryFireballEntity(
                this.level,
                target,
                startVec.x, startVec.y, startVec.z
        );
        this.level.addFreshEntity(fireball);

        return true;
    }

    private boolean shootArrowAtNearestTarget() {
        if (this.level == null) return false;
        ServerLevel serverLevel = (ServerLevel) this.level;
        BlockPos pos = this.getBlockPos();

        // Ищем ближайшую живую сущность (не игрока? можно исключить по желанию)
        Vec3 startVec = new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
        AABB searchArea = new AABB(pos).inflate(TARGET_RADIUS);
        List<LivingEntity> candidates = this.level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e.isAlive() && !e.equals(null)); // можно добавить фильтр: !(e instanceof Player)

        if (candidates.isEmpty()) return false;

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(startVec)))
                .orElse(null);
        if (target == null) return false;

        // Создаём стрелу
        Arrow arrow = EntityType.ARROW.create(this.level);
        if (arrow == null) return false;

        arrow.setPos(startVec);
        arrow.setOwner(null); // можно назначить "владельцем" блок, но тогда стрела будет иметь тег блока
        Vec3 targetVec = target.getEyePosition(1.0F);
        Vec3 direction = targetVec.subtract(startVec).normalize();
        arrow.shoot(direction.x, direction.y, direction.z, 1.5f, 2.0f); // скорость и разброс
        this.level.addFreshEntity(arrow);

        return true;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Rune Action Block");
    }

    @Override
    public @org.jetbrains.annotations.Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RuneActionMenu(containerId, playerInventory, this);
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

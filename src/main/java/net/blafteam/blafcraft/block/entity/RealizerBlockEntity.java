package net.blafteam.blafcraft.block.entity;

import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class RealizerBlockEntity extends BlockEntity {
    private int tickCounter = 0;
    private int effect_interval = 40;

    public final ItemStackHandler inventory = new ItemStackHandler(1) {

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private float rotation;

    public RealizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.REALIZER_BE.get(), pos, blockState);
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public float getRenderingRotation() {
        rotation += 0.5f;
        if(rotation >= 360) {
            rotation = 0;
        }
        return rotation;
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

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    // Статический метод, который будет вызываться каждый серверный тик для всех BE этого типа
    public static void serverTick(Level level, BlockPos pos, BlockState state, RealizerBlockEntity entity) {
        if (level.isClientSide || entity.inventory.getStackInSlot(0).isEmpty()) return;

        entity.tickCounter++;

        if (entity.tickCounter >= 40) {
            entity.tickCounter = 0;
            applyEffectToNearbyPlayers((ServerLevel) level, pos, entity.inventory);
        }
    }

    private static void applyEffectToNearbyPlayers(ServerLevel level, BlockPos pos, ItemStackHandler inventory) {
        double radius = 20.0;
        double radiusSq = radius * radius; // квадрат радиуса для быстрой проверки
        Vec3 center = pos.getCenter(); // центр блока

        Item orb = inventory.getStackInSlot(0).getItem();


        Holder<MobEffect> effect = null;
        if (orb == ModItems.RED_FACTON.get()) {
            effect = MobEffects.DAMAGE_BOOST;
        } else if (orb == ModItems.ORANGE_FACTON.get()) {
            effect = MobEffects.FIRE_RESISTANCE;
        } else if (orb == ModItems.YELLOW_FACTON.get()) {
            effect = MobEffects.DIG_SPEED;
        } else if (orb == ModItems.GREEN_FACTON.get()) {
            effect = MobEffects.JUMP;
        } else if (orb == ModItems.BLUE_FACTON.get()) {
            effect = MobEffects.MOVEMENT_SPEED;
        } else if (orb == ModItems.PURPLE_FACTON.get()) {
            effect = MobEffects.DAMAGE_RESISTANCE;
        } else if (orb == ModItems.PINK_FACTON.get()) {
            effect = MobEffects.REGENERATION;
        } else if (orb == ModItems.LIGHT_FACTON.get()) {
            effect = MobEffects.GLOWING;
        } else if (orb == ModItems.DARK_FACTON.get()) {
            effect = MobEffects.DARKNESS;
        }

        for (Player player : level.players()) {
            if (player instanceof ServerPlayer sp) {
                // Проверяем расстояние от игрока до центра блока
                if (sp.position().distanceToSqr(center) <= radiusSq) {
                    assert effect != null;
                    sp.addEffect(new MobEffectInstance(effect, 60, 0, true, false, true));
                }
            }
        }
    }
}

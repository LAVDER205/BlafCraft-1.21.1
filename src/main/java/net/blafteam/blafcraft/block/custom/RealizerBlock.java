package net.blafteam.blafcraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.blafteam.blafcraft.block.entity.RealizerBlockEntity;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.item.custom.FactonItem;
import net.blafteam.blafcraft.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RealizerBlock extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    public static final MapCodec<RealizerBlock> CODEC = simpleCodec(RealizerBlock::new);
    private final int interval = 20;

    public RealizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // BLOCK ENTITY

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // or it will be invisible
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RealizerBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof RealizerBlockEntity realizerBlockEntity ) {
                realizerBlockEntity.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof RealizerBlockEntity realizerBlockEntity) {
            if (realizerBlockEntity.inventory.getStackInSlot(0).isEmpty() && stack.getItem() instanceof FactonItem) {
                realizerBlockEntity.inventory.insertItem(0, stack.copy(), false);
                stack.shrink(1);
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
            } else if (stack.isEmpty() && !realizerBlockEntity.inventory.getStackInSlot(0).isEmpty()) {
                ItemStack stackInRealizer = realizerBlockEntity.inventory.extractItem(0, 1, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, stackInRealizer);
                realizerBlockEntity.clearContents();
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 1f);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    // PARTICLES

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, interval); // запланировать первый тик
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RealizerBlockEntity realizerBlockEntity && !realizerBlockEntity.inventory.getStackInSlot(0).isEmpty()) {
            level.sendParticles(ModParticles.TELEPORT_PARTICLES.get(),
                    pos.getX() + 0.5, pos.getY() + 1.1875, pos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.1);

            level.sendParticles(ModParticles.BLOOD_PARTICLES.get(),
                    pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5,
                    10, 4.5, 0, 4.5, 0.3);
        }
        level.scheduleTick(pos, this, interval);
    }
}

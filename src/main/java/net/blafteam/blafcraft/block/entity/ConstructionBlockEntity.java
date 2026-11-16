package net.blafteam.blafcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ConstructionBlockEntity extends BlockEntity {
    private long expiresAt;

    public ConstructionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSTRUCTION.get(), pos, state);
    }

    public void expireAt(long tick) {
        expiresAt = tick;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConstructionBlockEntity construction) {
        // Время хранится рядом с блоком. При загрузке чанка старое укрытие сразу исчезнет.
        if (level.getGameTime() >= construction.expiresAt) level.removeBlock(pos, false);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        expiresAt = tag.getLong("ExpiresAt");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ExpiresAt", expiresAt);
    }
}

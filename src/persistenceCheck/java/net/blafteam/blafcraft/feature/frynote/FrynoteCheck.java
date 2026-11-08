package net.blafteam.blafcraft.feature.frynote;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class FrynoteCheck {
    public static void main(String[] args) {
        FrynoteData state = new FrynoteData();
        state.spendCharge();
        tick(state, 20);
        state.spendCharge();
        state.spendCharge();
        check(state.charges() == 0 && state.recharge() == 60, "spending preserves current recharge");
        tick(state, 59);
        check(state.charges() == 0, "no early charge");
        tick(state, 1);
        check(state.charges() == 1 && state.recharge() == 80, "sequential recharge");
        tick(state, 160);
        check(state.charges() == 3 && state.recharge() == 0, "full recharge");

        state.markCombat();
        check(state.regenPerTick() == 0.1F, "two mana per second");
        tick(state, 119);
        check(state.inCombat(), "six-second combat window");
        tick(state, 1);
        check(state.regenPerTick() == 0.25F, "five mana per second after combat");

        UUID target = UUID.randomUUID();
        check(!state.recordMeleeHit(target, new Vec3(1, 0, 0)), "ordinary hit has no bonus");
        state.markTeleport();
        check(!state.recordMeleeHit(target, new Vec3(1, 0, 0)), "same angle has no bonus");
        check(state.recordMeleeHit(target, new Vec3(-1, 0, 0)), "different side after teleport");
        state.markTeleport();
        check(!state.recordMeleeHit(target, new Vec3(1, 0, 0)), "bonus cooldown");
        tick(state, 60);
        check(!state.recordMeleeHit(target, new Vec3(-1, 0, 0)), "expired teleport window");
        state.markTeleport();
        check(state.recordMeleeHit(target, new Vec3(1, 0, 0)), "bonus after next maneuver");
        tick(state, 60);
        state.markTeleport();
        check(!state.recordMeleeHit(UUID.randomUUID(), new Vec3(-1, 0, 0)), "same target is required");

        state.setAnchor(Vec3.ZERO, Level.OVERWORLD);
        tick(state, 159);
        check(state.anchor() != null, "anchor remains during its window");
        tick(state, 1);
        check(state.anchor() == null && state.anchorCooldown() == 320, "anchor expiry starts cooldown");
        tick(state, 320);
        check(state.anchorCooldown() == 0, "anchor cooldown expires");

        state.spendCharge();
        state.startGlanceCooldown();
        state.setAnchor(Vec3.ZERO, Level.OVERWORLD);
        CompoundTag tag = state.serializeNBT(RegistryAccess.EMPTY);
        FrynoteData restored = new FrynoteData();
        restored.deserializeNBT(RegistryAccess.EMPTY, tag);
        check(restored.charges() == 2 && restored.recharge() == 80 && restored.glanceCooldown() == 160,
                "timers survive reload");
        check(restored.anchor() == null && restored.anchorCooldown() == 320, "reload cannot restore an active anchor");
        tag.putInt("Charges", 100);
        tag.putInt("GlanceCooldown", -100);
        restored.deserializeNBT(RegistryAccess.EMPTY, tag);
        check(restored.charges() == 3 && restored.glanceCooldown() == 0, "invalid stored values are clamped");
        restored.deserializeNBT(RegistryAccess.EMPTY, new CompoundTag());
        check(restored.charges() == 3, "old player defaults");
        System.out.println("Frynote charge, mana, maneuver and anchor checks passed.");
    }

    private static void tick(FrynoteData data, int count) {
        for (int i = 0; i < count; i++) data.tick();
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}

package net.blafteam.blafcraft.feature.frynote;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.UUID;

/** Данные принадлежат игроку, поэтому счётчики разных миров и игроков не смешиваются. */
public final class FrynoteData implements INBTSerializable<CompoundTag> {
    private int charges = FrynoteBalance.MAX_CHARGES;
    private int recharge;
    private int glanceCooldown;
    private int anchorCooldown;
    private int anchorTicks;
    private int combatTicks;
    private int bonusCooldown;
    private int maneuverTicks;
    private int previousHitTicks;
    private UUID previousTarget;
    private Vec3 previousDirection;
    private Vec3 anchor;
    private ResourceKey<Level> anchorDimension;

    public int charges() { return charges; }
    public int recharge() { return recharge; }
    public int glanceCooldown() { return glanceCooldown; }
    public int anchorCooldown() { return anchorCooldown; }
    public int anchorTicks() { return anchorTicks; }
    public boolean inCombat() { return combatTicks > 0; }
    public float regenPerTick() { return inCombat() ? FrynoteBalance.COMBAT_REGEN : FrynoteBalance.REST_REGEN; }
    public Vec3 anchor() { return anchor; }
    public ResourceKey<Level> anchorDimension() { return anchorDimension; }

    public void tick() {
        if (charges < FrynoteBalance.MAX_CHARGES && --recharge <= 0) {
            charges++;
            recharge = charges < FrynoteBalance.MAX_CHARGES ? FrynoteBalance.CHARGE_TICKS : 0;
        }
        glanceCooldown = Math.max(0, glanceCooldown - 1);
        anchorCooldown = Math.max(0, anchorCooldown - 1);
        combatTicks = Math.max(0, combatTicks - 1);
        bonusCooldown = Math.max(0, bonusCooldown - 1);
        maneuverTicks = Math.max(0, maneuverTicks - 1);
        previousHitTicks = Math.max(0, previousHitTicks - 1);
        if (anchorTicks > 0 && --anchorTicks == 0) cancelAnchor();
    }

    public void spendCharge() {
        if (charges <= 0) throw new IllegalStateException("Нет заряда рывка");
        if (charges == FrynoteBalance.MAX_CHARGES) recharge = FrynoteBalance.CHARGE_TICKS;
        charges--;
    }

    public void startGlanceCooldown() { glanceCooldown = FrynoteBalance.GLANCE_COOLDOWN; }

    public void setAnchor(Vec3 position, ResourceKey<Level> dimension) {
        anchor = position;
        anchorDimension = dimension;
        anchorTicks = FrynoteBalance.ANCHOR_LIFETIME;
    }

    public void cancelAnchor() {
        if (anchor == null && anchorTicks == 0) return;
        anchor = null;
        anchorDimension = null;
        anchorTicks = 0;
        anchorCooldown = FrynoteBalance.ANCHOR_COOLDOWN;
    }

    public void markCombat() { combatTicks = FrynoteBalance.COMBAT_TICKS; }
    public void markTeleport() { maneuverTicks = FrynoteBalance.MANEUVER_WINDOW; }

    /** Вызывается только после настоящего урона от удара. Один перенос даёт не больше одного бонуса. */
    public boolean recordMeleeHit(UUID target, Vec3 direction) {
        boolean bonus = maneuverTicks > 0 && bonusCooldown == 0 && previousHitTicks > 0
                && target.equals(previousTarget) && previousDirection != null
                && direction.lengthSqr() > 0.01 && previousDirection.lengthSqr() > 0.01
                && previousDirection.dot(direction) <= FrynoteBalance.SIDE_CHANGE_DOT;
        previousTarget = target;
        previousDirection = direction;
        previousHitTicks = FrynoteBalance.COMBAT_TICKS;
        if (bonus) {
            maneuverTicks = 0;
            bonusCooldown = FrynoteBalance.BONUS_COOLDOWN;
        }
        return bonus;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Charges", charges);
        tag.putInt("Recharge", recharge);
        tag.putInt("GlanceCooldown", glanceCooldown);
        // Активный якорь не переживает перезапуск/смерть. Его кулдаун сохраняется.
        tag.putInt("AnchorCooldown", anchor != null ? FrynoteBalance.ANCHOR_COOLDOWN : anchorCooldown);
        tag.putInt("Combat", combatTicks);
        tag.putInt("BonusCooldown", bonusCooldown);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        charges = tag.contains("Charges") ? Math.clamp(tag.getInt("Charges"), 0, FrynoteBalance.MAX_CHARGES)
                : FrynoteBalance.MAX_CHARGES;
        recharge = charges == FrynoteBalance.MAX_CHARGES ? 0
                : Math.clamp(tag.getInt("Recharge"), 1, FrynoteBalance.CHARGE_TICKS);
        glanceCooldown = Math.clamp(tag.getInt("GlanceCooldown"), 0, FrynoteBalance.GLANCE_COOLDOWN);
        anchorCooldown = Math.clamp(tag.getInt("AnchorCooldown"), 0, FrynoteBalance.ANCHOR_COOLDOWN);
        combatTicks = Math.clamp(tag.getInt("Combat"), 0, FrynoteBalance.COMBAT_TICKS);
        bonusCooldown = Math.clamp(tag.getInt("BonusCooldown"), 0, FrynoteBalance.BONUS_COOLDOWN);
        anchor = null;
        anchorDimension = null;
        anchorTicks = 0;
        maneuverTicks = 0;
        previousHitTicks = 0;
        previousTarget = null;
        previousDirection = null;
    }
}

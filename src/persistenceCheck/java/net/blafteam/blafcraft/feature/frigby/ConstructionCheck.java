package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.keybinds.ActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class ConstructionCheck {
    public static void main(String[] args) {
        var selected = new FrigbyLoadout(ActionType.STONE_WALL, ActionType.CANNON, ActionType.ARROW_RAIN);
        check(FrigbyLoadout.DEFAULT.isValid(), "old characters have a playable default");
        check(selected.equals(FrigbyLoadout.decode(selected.encode())), "six constructions survive save and sync");
        check(FrigbyLoadout.decode("ARROW,ARROW,CANNON") == null, "reject duplicate slots");
        check(FrigbyLoadout.decode("ARROW,KING_CALL,CANNON") == null, "keep King's Call separate");
        check(FrigbyLoadout.decode("ARROW,UNKNOWN,CANNON") == null, "reject unknown abilities");

        var rearranged = new FrigbyLoadout(ActionType.FIREBALL, ActionType.CREATION_STEP, ActionType.ARROW);
        check(FrigbyLoadout.DEFAULT.affectedBy(rearranged).equals(Set.of(ActionType.FIREBALL, ActionType.CREATION_STEP)),
                "unchanged slot does not block a swap");
        var replacement = new FrigbyLoadout(ActionType.STONE_WALL, ActionType.FIREBALL, ActionType.ARROW);
        check(FrigbyLoadout.DEFAULT.affectedBy(replacement).equals(Set.of(ActionType.CREATION_STEP, ActionType.STONE_WALL)),
                "replacing a slot respects both constructions' cooldowns");

        var anchor = new BlockPos(-7, 64, 13);
        check(Set.copyOf(ConstructionAim.wallBlocks(anchor, Direction.NORTH)).equals(Set.of(
                anchor.west(), anchor.west().above(), anchor, anchor.above(), anchor.east(), anchor.east().above())),
                "wall faces north across three columns");
        check(Set.copyOf(ConstructionAim.wallBlocks(anchor, Direction.WEST)).equals(Set.of(
                anchor.north(), anchor.north().above(), anchor, anchor.above(), anchor.south(), anchor.south().above())),
                "wall rotates with the caster");
        var rain = FrigbyConstructions.rainColumns(anchor);
        int dedicatedCenterShots = (FrigbyBalance.RAIN_DURATION_TICKS + 11) / 12;
        check(FrigbyBalance.RAIN_DURATION_TICKS - dedicatedCenterShots >= rain.size(),
                "one cast covers every column in addition to repeated center shots");
        check(rain.contains(anchor.east(4)) && rain.contains(anchor.north(4)) && !rain.contains(anchor.east(4).south(4))
                && Set.copyOf(rain).size() == rain.size(), "rain covers a circle without duplicate columns");

        Vec3 muzzle = new Vec3(-3, 64, 8);
        Vec3 nearHit = muzzle.add(5, 0, 0);
        Vec3 overshoot = muzzle.add(100, 0, 0);
        check(FrigbyConstructions.capBlastPosition(muzzle, nearHit).equals(nearHit), "collision before fuse range");
        check(Math.abs(FrigbyConstructions.capBlastPosition(muzzle, overshoot).distanceTo(muzzle)
                - FrigbyBalance.CANNON_SHOT_RANGE) < 0.0001, "blast stays at fuse range even when a tick overshoots");

        System.out.println("Frigby loadout, wall geometry, rain coverage and cannon range checks passed.");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}

package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.keybinds.ActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

public final class ConstructionCheck {
    public static void main(String[] args) {
        var selected = new FrigbyLoadout(ActionType.STONE_WALL, ActionType.CANNON, ActionType.ARROW_RAIN);
        check(FrigbyLoadout.DEFAULT.isValid(), "old characters have a playable default");
        check(selected.equals(FrigbyLoadout.decode(selected.encode())), "six constructions survive save and sync");
        check(FrigbyLoadout.decode("ARROW,ARROW,CANNON") == null, "reject duplicate slots");
        check(FrigbyLoadout.decode("ARROW,KING_CALL,CANNON") == null, "keep King's Call separate");
        check(FrigbyLoadout.decode("ARROW,UNKNOWN,CANNON") == null, "reject unknown abilities");

        var anchor = new BlockPos(-7, 64, 13);
        check(Set.copyOf(ConstructionAim.wallBlocks(anchor, Direction.NORTH)).equals(Set.of(
                anchor.west(), anchor.west().above(), anchor, anchor.above(), anchor.east(), anchor.east().above())),
                "wall faces north across three columns");
        check(Set.copyOf(ConstructionAim.wallBlocks(anchor, Direction.WEST)).equals(Set.of(
                anchor.north(), anchor.north().above(), anchor, anchor.above(), anchor.south(), anchor.south().above())),
                "wall rotates with the caster");
        System.out.println("Frigby construction loadout and wall geometry checks passed.");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}

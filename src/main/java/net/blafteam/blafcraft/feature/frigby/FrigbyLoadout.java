package net.blafteam.blafcraft.feature.frigby;

import com.mojang.serialization.Codec;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record FrigbyLoadout(ActionType first, ActionType second, ActionType third) {
    public static final List<ActionType> CONSTRUCTIONS = List.of(ActionType.CREATION_STEP, ActionType.FIREBALL,
            ActionType.ARROW, ActionType.STONE_WALL, ActionType.ARROW_RAIN, ActionType.CANNON);
    public static final FrigbyLoadout DEFAULT = new FrigbyLoadout(ActionType.CREATION_STEP, ActionType.FIREBALL, ActionType.ARROW);
    public static final Codec<FrigbyLoadout> CODEC = Codec.STRING.xmap(FrigbyLoadout::readSaved, FrigbyLoadout::encode);

    public ActionType forSlot(int slot) {
        return switch (slot) {
            case 0 -> first;
            case 1 -> second;
            case 2 -> third;
            default -> null;
        };
    }

    public boolean includes(ActionType action) {
        return first == action || second == action || third == action;
    }

    public boolean isValid() {
        return CONSTRUCTIONS.contains(first) && CONSTRUCTIONS.contains(second) && CONSTRUCTIONS.contains(third)
                && first != second && first != third && second != third;
    }

    public String encode() {
        return first.name() + "," + second.name() + "," + third.name();
    }

    public static FrigbyLoadout decode(String value) {
        String[] names = value.split(",", -1);
        if (names.length != 3) return null;
        try {
            var loadout = new FrigbyLoadout(ActionType.valueOf(names[0]), ActionType.valueOf(names[1]), ActionType.valueOf(names[2]));
            return loadout.isValid() ? loadout : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static FrigbyLoadout readSaved(String value) {
        FrigbyLoadout loadout = decode(value);
        return loadout == null ? DEFAULT : loadout;
    }

    public static FrigbyLoadout get(ServerPlayer player) {
        return player.getData(ModAttachments.FRIGBY_LOADOUT.get());
    }

    public void apply(ServerPlayer player) {
        player.setData(ModAttachments.FRIGBY_LOADOUT.get(), this);
        // Если шаг убрали из набора, его нельзя оставить включённым до следующей смены слотов.
        if (!includes(ActionType.CREATION_STEP)) player.removeEffect(ModEffects.CREATION_STEP_EFFECT);
    }
}

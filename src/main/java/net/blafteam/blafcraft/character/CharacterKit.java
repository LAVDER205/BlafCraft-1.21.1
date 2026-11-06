package net.blafteam.blafcraft.character;

import net.blafteam.blafcraft.keybinds.ActionType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Чтобы добавить персонажа, добавь строку с уникальным ID и действиями для Z, X, C.
 * null означает пустой слот. ID записывается в мир, поэтому не переименовывай его
 * после выдачи персонажа без переноса старых сохранений.
 */
public enum CharacterKit {
    NONE("none", null, null, null),
    FRIGBY("frigby", ActionType.CREATION_STEP, ActionType.FIREBALL, ActionType.ARROW),
    FRYNOTE("frynote", ActionType.TELEPORT_DASH, ActionType.TELEPORT_DASH, ActionType.TELEPORT_DASH),
    LAVDER("lavder", ActionType.BLOODLUST, ActionType.BLOODLUST, ActionType.BLOODLUST),
    DEV("dev", ActionType.OVERDOSE_ABILITY, ActionType.TEST, ActionType.TEST);

    private final String id;
    private final ActionType z;
    private final ActionType x;
    private final ActionType c;

    CharacterKit(String id, ActionType z, ActionType x, ActionType c) {
        this.id = id;
        this.z = z;
        this.x = x;
        this.c = c;
    }

    public String id() {
        return id;
    }

    public ActionType actionForSlot(int slot) {
        return switch (slot) {
            case 0 -> z;
            case 1 -> x;
            case 2 -> c;
            default -> null;
        };
    }

    public static Optional<CharacterKit> byId(String id) {
        return Arrays.stream(values()).filter(kit -> kit.id.equals(id)).findFirst();
    }
}

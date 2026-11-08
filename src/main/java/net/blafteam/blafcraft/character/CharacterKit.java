package net.blafteam.blafcraft.character;

import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.AbilitySlots;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисли действия в порядке слотов. null оставляет пустой слот посередине.
 * ID записывается в мир, поэтому не переименовывай его
 * после выдачи персонажа без переноса старых сохранений.
 */
public enum CharacterKit {
    NONE("none"),
    FRIGBY("frigby", ActionType.CREATION_STEP, ActionType.FIREBALL, ActionType.ARROW),
    FRYNOTE("frynote", ActionType.TELEPORT_DASH, ActionType.TELEPORT_GLANCE, ActionType.TELEPORT_ANCHOR),
    LAVDER("lavder", ActionType.OVERDOSE_ABILITY),
    DEV("dev", ActionType.TEST, ActionType.TEST, ActionType.TEST, ActionType.TEST);

    private final String id;
    private final ActionType[] actions;

    CharacterKit(String id, ActionType... actions) {
        if (actions.length > AbilitySlots.COUNT) {
            throw new IllegalArgumentException(id + ": доступно слотов: " + AbilitySlots.COUNT);
        }
        this.id = id;
        this.actions = actions;
    }

    public String id() {
        return id;
    }

    public ActionType actionForSlot(int slot) {
        return slot >= 0 && slot < actions.length ? actions[slot] : null;
    }

    // Добавил в набор способность с ценой маны — шкала появится автоматически.
    public boolean usesMana() {
        for (ActionType action : actions) {
            if (action != null && action.getMana_price() > 0) return true;
        }
        return false;
    }

    public static Optional<CharacterKit> byId(String id) {
        return Arrays.stream(values()).filter(kit -> kit.id.equals(id)).findFirst();
    }
}

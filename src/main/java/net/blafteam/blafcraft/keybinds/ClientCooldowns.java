package net.blafteam.blafcraft.keybinds;

import java.util.HashMap;
import java.util.Map;

public class ClientCooldowns {
        // Для каждого ActionType храним оставшееся число тиков (0 = готово)
        private static final Map<ActionType, Integer> remaining = new HashMap<>();

        public static void startCooldown(ActionType action) { remaining.put(action, action.getCooldownTicks()); } // запуск кд

        public static int getRemaining(ActionType action) {
            return remaining.getOrDefault(action, 0);
        } // вывод

        // Вызывать каждый клиентский тик
        public static void tick() {
            remaining.replaceAll((k, v) -> Math.max(0, v - 1));
        } // уменьшаем кд на 1
    }

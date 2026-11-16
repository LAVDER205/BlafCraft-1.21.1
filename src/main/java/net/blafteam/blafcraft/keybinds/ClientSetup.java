package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.duel.DuelShortcutPayload;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import net.blafteam.blafcraft.feature.frigby.ConstructionAim;
import net.blafteam.blafcraft.feature.frigby.ConstructionPreview;
import net.blafteam.blafcraft.feature.frigby.FrigbyLoadoutScreen;
import net.blafteam.blafcraft.hud.AbilityHud;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public class ClientSetup {
    private static final KeyMapping[] ABILITY_KEYS = createAbilityKeys();
    private static final KeyMapping LOADOUT_KEY = new KeyMapping("key.blafcraft.frigby_loadout",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.blafcraft");
    private static AimInput aiming;

    private record AimInput(int slot, ActionType action, ClientLevel level) {}

    private static final KeyMapping SOCIAL_KEY = new KeyMapping("key.blafcraft.social",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.blafcraft");
    private static final KeyMapping DUEL_KEY = new KeyMapping("key.blafcraft.duel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blafcraft");

    public static Component abilityKeyLabel(int slot) {
        var mapping = ABILITY_KEYS[slot];
        var key = mapping.getKey();
        // GLFW хранит код буквенной клавиши в латинице, даже если ОС сейчас печатает кириллицей.
        int code = key.getValue();
        if (key.getType() == InputConstants.Type.KEYSYM && code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) {
            return Component.literal(Character.toString('A' + code - GLFW.GLFW_KEY_A));
        }
        return mapping.getTranslatedKeyMessage();
    }

    private static KeyMapping[] createAbilityKeys() {
        KeyMapping[] keys = new KeyMapping[AbilitySlots.COUNT];
        for (int slot = 0; slot < keys.length; slot++) {
            int defaultKey = switch (slot) {
                case 0 -> GLFW.GLFW_KEY_Z;
                case 1 -> GLFW.GLFW_KEY_X;
                case 2 -> GLFW.GLFW_KEY_C;
                case 3 -> GLFW.GLFW_KEY_V;
                default -> GLFW.GLFW_KEY_UNKNOWN;
            };
            // Имя настройки и номер в пакете должны оставаться стабильными между версиями.
            keys[slot] = new KeyMapping("key.blafcraft.ability_" + (slot + 1),
                    KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, "key.categories.blafcraft");
        }
        return keys;
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SOCIAL_KEY);
        event.register(LOADOUT_KEY);
        event.register(DUEL_KEY);
        for (KeyMapping key : ABILITY_KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.player != null && mc.level != null && mc.getConnection() != null
                && mc.screen == null && !mc.isPaused() && mc.isWindowActive() && mc.player.isAlive();
        while (LOADOUT_KEY.consumeClick()) {
            if (inWorld && AbilityHud.isFrigby()) {
                aiming = null;
                mc.setScreen(new FrigbyLoadoutScreen());
                inWorld = false;
            }
        }

        for (int slot = 0; slot < ABILITY_KEYS.length; slot++) {
            // Для обычных действий оставляем нажатие. Для конструкций запоминаем слот до отпускания.
            while (ABILITY_KEYS[slot].consumeClick()) {
                if (!inWorld) continue;
                ActionType action = AbilityHud.actionForSlot(slot);
                aiming = null;
                if (action == null) continue;
                if (ConstructionAim.needsAim(action)) {
                    if (ABILITY_KEYS[slot].isDown()) aiming = new AimInput(slot, action, mc.level);
                } else {
                    PacketDistributor.sendToServer(new ActionPacket(slot, action.name()));
                }
            }
        }
        if (!inWorld) aiming = null;
        else updateAim(mc);
        while (SOCIAL_KEY.consumeClick()) {
            if (!inWorld) continue;
            // С приседанием всегда открываем список, даже если перед игроком кто-то стоит.
            if (!mc.player.isShiftKeyDown() && mc.hitResult instanceof EntityHitResult hit
                    && hit.getEntity() instanceof Player target) {
                PacketDistributor.sendToServer(new FriendUpdatePacket(target.getUUID(), true));
            } else {
                PacketDistributor.sendToServer(new FriendUpdatePacket(mc.player.getUUID(), false));
            }
        }
        while (DUEL_KEY.consumeClick()) {
            if (!inWorld) continue;
            // Без цели или с приседанием клавиша открывает управление текущей дуэлью.
            var target = !mc.player.isShiftKeyDown() && mc.hitResult instanceof EntityHitResult hit
                    && hit.getEntity() instanceof Player player ? player.getUUID() : mc.player.getUUID();
            PacketDistributor.sendToServer(new DuelShortcutPayload(target));
        }
    }

    private static void updateAim(Minecraft mc) {
        if (aiming == null) return;
        if (aiming.level() != mc.level || AbilityHud.actionForSlot(aiming.slot()) != aiming.action()) {
            aiming = null;
            return;
        }
        var target = ConstructionAim.fromLook(mc.player, aiming.action());
        if (ABILITY_KEYS[aiming.slot()].isDown()) {
            ConstructionPreview.draw(mc.level, mc.player, target);
            return;
        }

        if (ConstructionPreview.canCast(mc.player, target)) {
            PacketDistributor.sendToServer(new ActionPacket(aiming.slot(), aiming.action().name()));
        } else {
            mc.player.displayClientMessage(Component.literal("Конструкция недоступна"), true);
        }
        aiming = null;
    }
}

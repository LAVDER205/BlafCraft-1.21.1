package net.blafteam.blafcraft.hud;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.character.CharacterHudPayload;
import net.blafteam.blafcraft.character.CharacterKit;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.feature.frynote.FrynoteClientHud;
import net.blafteam.blafcraft.keybinds.AbilitySlots;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.ClientCooldowns;
import net.blafteam.blafcraft.keybinds.ClientSetup;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public final class AbilityHud {
    private static CharacterKit kit = CharacterKit.NONE;

    private AbilityHud() {}

    public static void handle(CharacterHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> kit = CharacterKit.byId(payload.kitId()).orElse(CharacterKit.NONE));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        kit = CharacterKit.NONE;
    }

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName()) || mc.player == null || mc.options.hideGui) return;

        List<String> rows = new ArrayList<>();
        for (int slot = 0; slot < AbilitySlots.COUNT; slot++) {
            ActionType action = kit.actionForSlot(slot);
            if (action == null) continue;
            rows.add("[" + ClientSetup.abilityKeyLabel(slot).getString() + "] "
                    + abilityName(action) + ": " + status(mc, action));
        }

        // Нижняя строка остаётся на одной высоте и не перекрывает шкалу маны.
        int y = event.getGuiGraphics().guiHeight() - 48 - 12 * (rows.size() - 1);
        for (String row : rows) {
            event.getGuiGraphics().drawString(mc.font, row, 10, y, 0x99CCFF);
            y += 12;
        }
    }

    private static String status(Minecraft mc, ActionType action) {
        // По умолчанию показываем общий таймер. Особые состояния остаются в модуле персонажа.
        if (kit == CharacterKit.FRYNOTE) return FrynoteClientHud.status(action);
        if (action == ActionType.CREATION_STEP) {
            return mc.player.hasEffect(ModEffects.CREATION_STEP_EFFECT) ? "вкл." : "выкл.";
        }
        return ready(ClientCooldowns.getRemaining(action));
    }

    private static String abilityName(ActionType action) {
        return switch (action) {
            case TEST -> "Тест";
            case ARROW -> "Залп стрел";
            case FIREBALL -> "Фаербол";
            case CREATION_STEP -> "Созидающий шаг";
            case TELEPORT_DASH -> "Рывок";
            case TELEPORT_GLANCE -> "Взгляд";
            case TELEPORT_ANCHOR -> "Якорь";
            case BLOODLUST -> "Жажда крови";
            case TIME_BOMB -> "Бомба замедленного действия";
            case FLIGHT -> "Полёт";
            case SCULK_MARK -> "Скалковая метка";
            case MORPH -> "Морф";
            case OVERDOSE_ABILITY -> "Стимулятор";
        };
    }

    public static String ready(int ticks) {
        return ticks > 0 ? seconds(ticks) : "готово";
    }

    public static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f с", ticks / 20.0);
    }
}

package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public class ClientSetup {
    private static final KeyMapping[] ABILITY_KEYS = createAbilityKeys();

    private static KeyMapping[] createAbilityKeys() {
        KeyMapping[] keys = new KeyMapping[AbilitySlots.COUNT];
        for (int slot = 0; slot < keys.length; slot++) {
            int defaultKey = switch (slot) {
                case 0 -> GLFW.GLFW_KEY_Z;
                case 1 -> GLFW.GLFW_KEY_X;
                case 2 -> GLFW.GLFW_KEY_C;
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
        for (KeyMapping key : ABILITY_KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.player != null && mc.getConnection() != null && mc.screen == null && !mc.isPaused();
        for (int slot = 0; slot < ABILITY_KEYS.length; slot++) {
            // Считываем назначение Minecraft, включая кнопки мыши. В меню поглощаем нажатия.
            while (ABILITY_KEYS[slot].consumeClick()) {
                if (inWorld) PacketDistributor.sendToServer(new ActionPacket(slot));
            }
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || event.getAction() != GLFW.GLFW_PRESS) return;

        if (event.getKey() == GLFW.GLFW_KEY_B
                && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target) {
            PacketDistributor.sendToServer(new FriendUpdatePacket(target.getUUID(), true));
        }
    }
}

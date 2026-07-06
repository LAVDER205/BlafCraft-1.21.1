package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientSetup {

    // Регистрация пакета (без изменений, но используем новый ActionPacket)
    @EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(
                    ActionPacket.TYPE,
                    ActionPacket.STREAM_CODEC,
                    ServerHandler::handle
            );
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String nickname = mc.player.getName().getString();

        if (Minecraft.getInstance().screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        ActionType action = null;
        if (event.getKey() == GLFW.GLFW_KEY_Z) {
            switch (nickname) {
                case "FRIGBY13" -> {
                    action = ActionType.CREATION_STEP;
                }
                case "Dev" -> {
                    action = ActionType.TELEPORT_DASH;
                }
            }
        }
        else if (event.getKey() == GLFW.GLFW_KEY_X) switch (nickname) {
            case "FRIGBY13" -> {
                action = ActionType.FIREBALL;
            }
            case "Dev" -> {
                action = ActionType.TELEPORT_DASH;
            }
        }
        else if (event.getKey() == GLFW.GLFW_KEY_C) switch (nickname) {
            case "FRIGBY13" -> {
                action = ActionType.ARROW;
            }
            case "Dev" -> {
                action = ActionType.TELEPORT_DASH;
            }
        }

        if (action != null) { // сработала какая-то способность
            // Отправляем пакет на сервер (сервер сам решит, можно ли выполнить)
            PacketDistributor.sendToServer(new ActionPacket(action));
            }
        }
    }

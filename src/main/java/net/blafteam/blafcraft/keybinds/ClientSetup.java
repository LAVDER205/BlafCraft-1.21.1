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
        if (Minecraft.getInstance().screen != null) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getKey() == GLFW.GLFW_KEY_H) {
                PacketDistributor.sendToServer(new ActionPacket(ActionType.SPEED));
            } else if (event.getKey() == GLFW.GLFW_KEY_J) {
                PacketDistributor.sendToServer(new ActionPacket(ActionType.JUMP));
            }  else if (event.getKey() == GLFW.GLFW_KEY_K) {
            PacketDistributor.sendToServer(new ActionPacket(ActionType.ARROW));
            }
        }
    }
}

package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || event.getAction() != GLFW.GLFW_PRESS) return;

        // Клиент сообщает, какая клавиша нажата. Сервер сам выбирает действие из набора игрока.
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_Z -> PacketDistributor.sendToServer(new ActionPacket(0));
            case GLFW.GLFW_KEY_X -> PacketDistributor.sendToServer(new ActionPacket(1));
            case GLFW.GLFW_KEY_C -> PacketDistributor.sendToServer(new ActionPacket(2));
            case GLFW.GLFW_KEY_B -> {
                if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player target) {
                    PacketDistributor.sendToServer(new FriendUpdatePacket(target.getUUID(), true));
                }
            }
            default -> {}
        }
    }
}

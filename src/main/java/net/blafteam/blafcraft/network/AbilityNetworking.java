package net.blafteam.blafcraft.network;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.duel.DuelShortcutPayload;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import net.blafteam.blafcraft.social.SocialInteractions;
import net.blafteam.blafcraft.keybinds.ActionPacket;
import net.blafteam.blafcraft.keybinds.ClientCooldowns;
import net.blafteam.blafcraft.keybinds.CooldownSyncPayload;
import net.blafteam.blafcraft.keybinds.ServerHandler;
import net.blafteam.blafcraft.mana.ClientManaHandler;
import net.blafteam.blafcraft.mana.ManaSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class AbilityNetworking {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DuelShortcutPayload.TYPE, DuelShortcutPayload.STREAM_CODEC, DuelShortcutPayload::handle);
        // Этот пакет нужен обеим сторонам, в том числе выделенному серверу без клиента Minecraft.
        event.registrar("2").playToServer(ActionPacket.TYPE, ActionPacket.STREAM_CODEC, ServerHandler::handle);
        event.registrar("2").playToServer(FriendUpdatePacket.TYPE, FriendUpdatePacket.STREAM_CODEC,
                SocialInteractions::handleFriendShortcut);
        // Выделенный сервер тоже регистрирует пакет, хотя обработка выполняется на клиенте.
        event.registrar("1").playToClient(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC, ClientManaHandler::handle);
        event.registrar("1").playToClient(
                CooldownSyncPayload.TYPE, CooldownSyncPayload.STREAM_CODEC, ClientCooldowns::handle);
    }
}

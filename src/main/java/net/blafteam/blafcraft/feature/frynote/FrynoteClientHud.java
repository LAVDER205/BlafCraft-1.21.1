package net.blafteam.blafcraft.feature.frynote;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.Minecraft;
import net.blafteam.blafcraft.hud.AbilityHud;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public final class FrynoteClientHud {
    private static FrynoteHudPayload snapshot;

    public static void handle(FrynoteHudPayload value, IPayloadContext context) {
        context.enqueueWork(() -> snapshot = value.enabled() ? value : null);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (snapshot == null || Minecraft.getInstance().isPaused()) return;
        var state = snapshot;
        snapshot = new FrynoteHudPayload(true, state.charges(), decrement(state.recharge()), decrement(state.glance()),
                decrement(state.anchorLife()), decrement(state.anchorCooldown()));
    }

    private static int decrement(int ticks) { return Math.max(0, ticks - 1); }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { snapshot = null; }

    public static String status(ActionType action) {
        var state = snapshot;
        if (state == null) return "…";
        return switch (action) {
            case TELEPORT_DASH -> {
                String charges = state.charges() + "/" + FrynoteBalance.MAX_CHARGES;
                if (state.recharge() > 0) charges += "  +" + AbilityHud.seconds(state.recharge());
                yield charges;
            }
            case TELEPORT_GLANCE -> AbilityHud.ready(state.glance());
            case TELEPORT_ANCHOR -> state.anchorLife() > 0
                    ? "возврат " + AbilityHud.seconds(state.anchorLife())
                    : AbilityHud.ready(state.anchorCooldown());
            default -> AbilityHud.ready(0);
        };
    }
}

package net.blafteam.blafcraft.feature.frynote;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
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

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        // Событие вызывается для каждого слоя. Рисуем текст ровно один раз за кадр.
        if (!event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR)
                || snapshot == null || Minecraft.getInstance().player == null) return;
        var mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int y = mc.getWindow().getGuiScaledHeight() - 72;
        String charges = "Рывок: " + snapshot.charges() + "/" + FrynoteBalance.MAX_CHARGES;
        if (snapshot.recharge() > 0) charges += "  +" + seconds(snapshot.recharge());
        graphics.drawString(mc.font, charges, 10, y, 0x99CCFF);
        graphics.drawString(mc.font, "Взгляд: " + ready(snapshot.glance()), 10, y + 12, 0x99CCFF);
        String anchor = snapshot.anchorLife() > 0 ? "Возврат: " + seconds(snapshot.anchorLife()) : ready(snapshot.anchorCooldown());
        graphics.drawString(mc.font, "Якорь: " + anchor, 10, y + 24, 0x99CCFF);
    }

    private static String ready(int ticks) { return ticks > 0 ? seconds(ticks) : "готов"; }
    private static String seconds(int ticks) { return String.format(java.util.Locale.ROOT, "%.1f с", ticks / 20.0); }
}

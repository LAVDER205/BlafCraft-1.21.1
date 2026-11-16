package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.hud.AbilityHud;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.ClientSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class FrigbyLoadoutScreen extends Screen {
    private final List<CycleButton<ActionType>> choices = new ArrayList<>(3);
    private Button apply;
    private Button cancelPlan;

    public FrigbyLoadoutScreen() {
        super(Component.literal("Созидание Фригби"));
    }

    @Override
    protected void init() {
        var queued = FrigbyLoadoutClient.queued();
        FrigbyLoadout current = choices.size() == 3 ? selected() : (queued != null ? queued : FrigbyLoadoutClient.get());
        choices.clear();
        int left = width / 2 - 122;
        int top = height / 2 - 63;
        for (int slot = 0; slot < 3; slot++) {
            var choice = CycleButton.<ActionType>builder(action -> Component.literal(AbilityHud.abilityName(action)))
                    .withValues(FrigbyLoadout.CONSTRUCTIONS)
                    .withInitialValue(current.forSlot(slot))
                    .create(left, top + slot * 36, 244, 20, ClientSetup.abilityKeyLabel(slot),
                            (button, action) -> apply.active = selected().isValid());
            choices.add(addRenderableWidget(choice));
        }
        apply = addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> save())
                .bounds(left, top + 114, 76, 20).build());
        apply.active = selected().isValid();
        cancelPlan = addRenderableWidget(Button.builder(Component.literal("Отменить план"), button -> submit(""))
                .bounds(left + 80, top + 114, 92, 20).build());
        cancelPlan.active = queued != null;
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
                .bounds(left + 176, top + 114, 68, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        cancelPlan.active = FrigbyLoadoutClient.queued() != null;
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - 63;
        graphics.drawCenteredString(font, title, width / 2, top - 16, 0xFFFFFF);
        for (int slot = 0; slot < choices.size(); slot++) {
            ActionType action = choices.get(slot).getValue();
            String details = action == ActionType.CREATION_STEP
                    ? FrigbyBalance.STEP_XP_PER_TICK * 20 + " опыта/с в воздухе"
                    : action.getExperiencePointCost() + " опыта · КД " + AbilityHud.seconds(action.getCooldownTicks());
            graphics.drawCenteredString(font, details, width / 2, top + slot * 36 + 23, 0xC0D8F0);
        }
        String note = "Зов короля остаётся в четвёртом слоте";
        if (cancelPlan.active) note = "Смена набора запланирована";
        if (!selected().isValid()) note = "Выбери три разные конструкции";
        graphics.drawCenteredString(font, note, width / 2, top + 140, selected().isValid() ? 0xC0D8F0 : 0xFF9B9B);
    }

    private FrigbyLoadout selected() {
        return new FrigbyLoadout(choices.get(0).getValue(), choices.get(1).getValue(), choices.get(2).getValue());
    }

    private void save() {
        FrigbyLoadout loadout = selected();
        if (!loadout.isValid()) return;
        submit(loadout.encode());
    }

    private void submit(String choices) {
        PacketDistributor.sendToServer(FrigbyLoadoutPayload.request(choices));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

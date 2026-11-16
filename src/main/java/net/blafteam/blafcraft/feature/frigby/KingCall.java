package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.feature.frynote.SafeTeleport;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.keybinds.CooldownSyncPayload;
import net.blafteam.blafcraft.social.SocialData;
import net.blafteam.blafcraft.sound.AbilitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class KingCall {
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.9F, 0.04F, 0.06F), 1.4F);
    private static final Map<UUID, Call> activeCalls = new HashMap<>();

    private record Portal(Vec3 feet, Direction facing) {
        boolean contains(ServerPlayer player) {
            Vec3 offset = player.position().subtract(feet);
            return offset.horizontalDistanceSqr() <= 0.65 * 0.65 && Math.abs(offset.y) < 0.75;
        }
    }

    // Точки зафиксированы на момент открытия. Перемещение Фригби не двигает выход.
    private record Call(ServerLevel level, UUID faction, long expiresAt, Portal exit, Map<UUID, Portal> entrances) {}

    private KingCall() {}

    public static void cast(ServerPlayer caller) {
        if (!FrigbyController.isFrigby(caller) || !caller.isAlive() || caller.isSpectator()) return;
        if (remainingCooldown(caller) > 0) {
            syncCooldown(caller);
            return;
        }

        ServerLevel level = caller.serverLevel();
        var server = level.getServer();
        var social = SocialData.get(server);
        UUID callerId = caller.getUUID();
        UUID faction = social.factionKeyOf(callerId);
        if (!isFactionLeader(social, faction, callerId)) {
            tell(caller, "Зов доступен главе фракции");
            return;
        }

        Portal exit = findPortal(caller);
        if (exit == null) {
            tell(caller, "Нет безопасного места для выхода");
            return;
        }

        Map<UUID, Portal> entrances = new HashMap<>();
        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (!canReceive(social, faction, callerId, ally.getUUID(), availableIn(ally, level))) continue;
            Portal entrance = findPortal(ally);
            // Один игрок может использовать только свой вход. Рядом с выходом перенос уже не нужен.
            if (entrance != null && entrance.feet().distanceToSqr(exit.feet()) >= 1.0) {
                entrances.put(ally.getUUID(), entrance);
            }
        }
        if (entrances.isEmpty()) {
            tell(caller, "Нет союзников со свободным местом для входа");
            return;
        }

        activeCalls.put(callerId, new Call(level, faction, level.getGameTime() + FrigbyBalance.KING_CALL_DURATION, exit, entrances));
        long readyAt = server.overworld().getGameTime() + FrigbyBalance.KING_CALL_COOLDOWN;
        caller.setData(ModAttachments.KING_CALL_READY_AT.get(), readyAt);
        syncCooldown(caller);
        AbilitySounds.play(level, exit.feet(), SoundEvents.BEACON_ACTIVATE, 0.65F, 0.9F);
        tell(caller, "Зов короля открыт");
        for (var entrance : entrances.entrySet()) {
            AbilitySounds.play(level, entrance.getValue().feet(), SoundEvents.BEACON_ACTIVATE, 0.5F, 1.1F);
            UUID allyId = entrance.getKey();
            ServerPlayer ally = server.getPlayerList().getPlayer(allyId);
            if (ally != null) tell(ally, "Зов короля: войди в красный крест");
        }
    }

    static boolean isFactionLeader(SocialData social, UUID faction, UUID caller) {
        return faction != null && faction.equals(social.factionKeyOf(caller)) && social.isLeader(caller);
    }

    static boolean canReceive(SocialData social, UUID faction, UUID caller, UUID ally, boolean availableInDimension) {
        return availableInDimension && !caller.equals(ally) && isFactionLeader(social, faction, caller)
                && faction.equals(social.factionKeyOf(ally));
    }

    private static boolean availableIn(ServerPlayer player, ServerLevel level) {
        return player != null && player.serverLevel() == level && player.isAlive() && !player.isSpectator();
    }

    private static Portal findPortal(ServerPlayer player) {
        Direction behind = player.getDirection().getOpposite();
        Direction[] sides = {behind, behind.getClockWise(), behind.getCounterClockWise(), behind.getOpposite()};
        BlockPos origin = player.blockPosition();
        // Сначала проверяем места сзади, сбоку и спереди на текущей высоте, затем на ступень выше и ниже.
        for (int dy : new int[]{0, 1, -1}) {
            for (Direction side : sides) {
                Vec3 candidate = Vec3.atBottomCenterOf(origin.relative(side, 2).offset(0, dy, 0));
                if (safeStandingPosition(player, candidate)) return new Portal(candidate, side);
            }
        }
        return null;
    }

    private static boolean safeStandingPosition(ServerPlayer player, Vec3 position) {
        // Даже присевший игрок должен иметь возможность выпрямиться после переноса.
        return SafeTeleport.isSafe(player, position, true)
                && player.serverLevel().noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(position));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (activeCalls.isEmpty()) return;
        var server = event.getServer();
        var social = SocialData.get(server);
        var calls = activeCalls.entrySet().iterator();
        while (calls.hasNext()) {
            var entry = calls.next();
            UUID casterId = entry.getKey();
            Call call = entry.getValue();
            ServerLevel level = call.level();
            long now = level.getGameTime();
            ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
            if (now >= call.expiresAt() || !availableIn(caster, level) || !FrigbyController.isFrigby(caster)
                    || !isFactionLeader(social, call.faction(), casterId)) {
                calls.remove();
                continue;
            }

            var entrances = call.entrances().entrySet().iterator();
            while (entrances.hasNext()) {
                var entrance = entrances.next();
                UUID allyId = entrance.getKey();
                ServerPlayer ally = server.getPlayerList().getPlayer(allyId);
                if (!canReceive(social, call.faction(), casterId, allyId, availableIn(ally, level))) {
                    entrances.remove();
                    continue;
                }
                Portal portal = entrance.getValue();
                if (portal.contains(ally) && !ally.isPassenger() && !ally.isSleeping()
                        && safeStandingPosition(ally, call.exit().feet()) && SafeTeleport.move(ally, call.exit().feet())) {
                    AbilitySounds.play(level, portal.feet(), SoundEvents.CHORUS_FRUIT_TELEPORT, 0.5F, 1.1F);
                    AbilitySounds.play(ally, SoundEvents.CHORUS_FRUIT_TELEPORT, 0.7F, 1.3F);
                    entrances.remove();
                    continue;
                }
                if (now % 4 == 0) drawCross(level, portal);
            }

            if (call.entrances().isEmpty()) calls.remove();
            else if (now % 4 == 0) drawCross(level, call.exit());
        }
    }

    private static void drawCross(ServerLevel level, Portal portal) {
        // Частицы образуют вертикальный крест и ничего не ставят в мире.
        Vec3 center = portal.feet().add(0, 1, 0);
        Direction sideways = portal.facing().getClockWise();
        for (int i = -4; i <= 4; i++) dust(level, center.add(0, i * 0.22, 0));
        for (int i = -3; i <= 3; i++) {
            if (i != 0) dust(level, center.add(sideways.getStepX() * i * 0.22, 0, sideways.getStepZ() * i * 0.22));
        }
    }

    private static void dust(ServerLevel level, Vec3 point) {
        level.sendParticles(RED_DUST, point.x, point.y, point.z, 1, 0, 0, 0, 0);
    }

    private static int remainingCooldown(ServerPlayer player) {
        long now = player.serverLevel().getServer().overworld().getGameTime();
        return (int) Math.clamp(player.getData(ModAttachments.KING_CALL_READY_AT.get()) - now,
                0, FrigbyBalance.KING_CALL_COOLDOWN);
    }

    private static void syncCooldown(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CooldownSyncPayload(ActionType.KING_CALL, remainingCooldown(player)));
    }

    private static void tell(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && FrigbyController.isFrigby(player)) syncCooldown(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        activeCalls.clear();
    }
}

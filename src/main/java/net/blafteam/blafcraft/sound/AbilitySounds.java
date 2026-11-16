package net.blafteam.blafcraft.sound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class AbilitySounds {
    private AbilitySounds() {}

    public static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        play(player.serverLevel(), player.position(), sound, volume, pitch);
    }

    public static void play(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
        // Сервер передаёт звук выполнившему способность игроку и тем, кто находится поблизости.
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}

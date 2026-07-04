package net.blafteam.blafcraft.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

public class ClientLoopingSoundManager {
    private static final Map<SoundEvent, AbstractTickableSoundInstance> playingSounds = new HashMap<>();

    public static void start(SoundEvent sound, float volume, float pitch) { // методы запуска, остановки музыки
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        stop(sound); // если уже играет – остановим

        AbstractTickableSoundInstance instance = new AbstractTickableSoundInstance(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom()) {
            {
                // Блок инициализации анонимного класса – здесь мы можем обращаться к protected-полям
                this.looping = true;
                this.volume = volume;
                this.pitch = pitch;
            }

            @Override
            public void tick() {
                if (mc.player == null) {
                    this.stop();
                    return;
                }
                this.x = mc.player.getX();
                this.y = mc.player.getY();
                this.z = mc.player.getZ();
            }
        };

        mc.getSoundManager().play(instance);
        playingSounds.put(sound, instance);
    }

    public static void stop(SoundEvent sound) {
        AbstractTickableSoundInstance instance = playingSounds.remove(sound);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static void stopAll() {
        for (SoundEvent key : playingSounds.keySet()) {
            stop(key);
        }
    }
}

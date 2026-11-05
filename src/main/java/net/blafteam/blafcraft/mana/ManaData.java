package net.blafteam.blafcraft.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ManaData(float currentMana, int maxMana, float manaRegen) {
    public static final Codec<ManaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("current").forGetter(ManaData::currentMana),
            Codec.INT.fieldOf("maximum").forGetter(ManaData::maxMana),
            Codec.FLOAT.fieldOf("regeneration").forGetter(ManaData::manaRegen)
    ).apply(instance, ManaData::new));

    public static ManaData defaults() {
        return new ManaData(ManaConfig.MAX_MANA, ManaConfig.MAX_MANA, ManaConfig.MANA_REGEN);
    }
}

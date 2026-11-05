package net.blafteam.blafcraft.mana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import java.nio.file.Files;

/** Runs without a game window: save to disk, reload, and check non-default values. */
public class ManaPersistenceCheck {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("blafcraft-mana-check");
        try {
            for (var data : new ManaData[] {
                    ManaData.defaults(), new ManaData(37.5f, 250, 0.125f), new ManaData(0, 175, 0)}) {
                var root = new CompoundTag();
                root.put("mana", ManaData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow());
                var path = directory.resolve("player.dat");
                NbtIo.writeCompressed(root, path);
                var loaded = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                var restored = ManaData.CODEC.parse(NbtOps.INSTANCE, loaded.get("mana")).getOrThrow();
                if (!data.equals(restored)) throw new AssertionError("Mana changed after disk reload: " + restored);
            }
            System.out.println("Mana disk round-trip passed: defaults, custom maximum/regeneration, empty mana.");
        } finally {
            Files.deleteIfExists(directory.resolve("player.dat"));
            Files.deleteIfExists(directory);
        }
    }
}

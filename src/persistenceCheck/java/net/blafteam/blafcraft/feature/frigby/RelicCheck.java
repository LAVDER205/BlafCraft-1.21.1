package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.social.SocialData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class RelicCheck {
    public static void main(String[] args) {
        var social = new SocialData();
        UUID frigby = UUID.randomUUID();
        UUID ally = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        social.createFaction("north", "Север");
        social.createFaction("south", "Юг");
        social.join("north", frigby, "Frigby", true);
        social.join("north", ally, "Ally", false);
        social.join("south", outsider, "Outsider", true);
        social.rememberFrigby(frigby, true);
        check(social.canCraftFrigbyRelic(frigby) && social.canCraftFrigbyRelic(ally), "faction access");
        check(!social.canCraftFrigbyRelic(outsider), "another faction cannot craft");
        var saved = social.save(new CompoundTag(), RegistryAccess.EMPTY);
        var restored = SocialData.load(saved, RegistryAccess.EMPTY);
        check(restored.canCraftFrigbyRelic(ally), "access after restart with Frigby offline");
        restored.removeMember("north", ally);
        check(!restored.canCraftFrigbyRelic(ally), "leaving immediately revokes access");
        restored.rememberFrigby(frigby, false);
        check(!restored.canCraftFrigbyRelic(frigby), "reassigning Frigby revokes the recipe");

        check(RelicCharge.chargePerTick(0) > RelicCharge.chargePerTick(16), "accumulation slows at higher levels");
        check(RelicCharge.chargePerTick(30) == RelicCharge.chargePerTick(100) && RelicCharge.chargePerTick(100) > 0,
                "high levels still accumulate some experience");
        System.out.println("Relic faction access and charge rate checks passed.");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}

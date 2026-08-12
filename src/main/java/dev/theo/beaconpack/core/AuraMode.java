package dev.theo.beaconpack.core;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Who a given effect is projected to. Chosen per effect, not per pack: sharing Regeneration with
 * the group while keeping Haste to yourself is the intended kind of decision.
 *
 * @param minTier lowest pack tier that may select this mode
 * @param costMultiplier fuel cost relative to {@link #SELF}
 */
public enum AuraMode implements StringRepresentable {
    SELF("self", 1, 1.0),
    ALLIES("allies", 2, 2.5),
    TEAM("team", 2, 2.0),
    ALLIES_AND_PETS("allies_and_pets", 3, 3.0);

    public static final Codec<AuraMode> CODEC = StringRepresentable.fromEnum(AuraMode::values);

    private final String name;
    private final int minTier;
    private final double costMultiplier;

    AuraMode(String name, int minTier, double costMultiplier) {
        this.name = name;
        this.minTier = minTier;
        this.costMultiplier = costMultiplier;
    }

    public int minTier() {
        return minTier;
    }

    public double costMultiplier() {
        return costMultiplier;
    }

    /** True when this mode reaches beyond the carrier, i.e. when range actually matters. */
    public boolean isAura() {
        return this != SELF;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Who a given effect is projected to. Chosen per effect, not per beacon: sharing Regeneration with
 * the group while keeping Haste to yourself is the intended kind of decision.
 *
 * <p>The modes form a ladder. A beacon grants every mode up to its own {@code aura_rank}, and an
 * Attunement augment raises that rank — so what a tier shares on its own, and what it needs help
 * for, is a datapack decision rather than a number buried in here.
 *
 * <p>Team sits below Allies deliberately: it reaches fewer people and costs less, so it is the
 * cheaper first step out of keeping everything to yourself.
 *
 * @param rank           lowest aura rank a beacon must reach to offer this mode
 * @param costMultiplier fuel cost relative to {@link #SELF}
 */
public enum AuraMode implements StringRepresentable {
    SELF("self", 0, 1.0),
    TEAM("team", 1, 1.7),
    ALLIES("allies", 2, 2.0),
    ALLIES_AND_PETS("allies_and_pets", 3, 2.4);

    public static final Codec<AuraMode> CODEC = StringRepresentable.fromEnum(AuraMode::values);

    private final String name;
    private final int rank;
    private final double costMultiplier;

    AuraMode(String name, int rank, double costMultiplier) {
        this.name = name;
        this.rank = rank;
        this.costMultiplier = costMultiplier;
    }

    public int rank() {
        return rank;
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

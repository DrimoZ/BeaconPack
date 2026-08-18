package dev.drimoz.portablebeacons.core;

import java.util.Set;

/**
 * A beacon tier's stats after its augments have been applied. Produced by {@link BeaconResolver} and
 * consumed by the ticking logic, the GUI and the tooltip alike, so the number the player reads is
 * always the number the server charges.
 *
 * @param effectSlots      configurable effect slots
 * @param augmentSlots     unlocked augment slots
 * @param range            aura radius in blocks
 * @param fuelCapacity     buffer size in fuel units
 * @param maxAmplifier     highest amplifier any effect may reach
 * @param fuelMultiplier   global factor on consumption (Efficiency lowers it)
 * @param allowedAuraModes aura modes the player may pick
 */
public record BeaconStats(
        int effectSlots,
        int augmentSlots,
        double range,
        int fuelCapacity,
        int maxAmplifier,
        double fuelMultiplier,
        double auraCostMultiplier,
        int freeEffectSlots,
        double movingCostMultiplier,
        double stillCostMultiplier,
        Set<AuraMode> allowedAuraModes,
        boolean hideParticles,
        boolean hideIcon
) {
    /**
     * The most effect slots a beacon can ever have, however generous the tier and the augments.
     *
     * <p>Lives here rather than in the screen because it is not only a drawing limit: an effect
     * beyond what the screen lays out would still be resolved, charged and kept by sanitize — paid
     * for and invisible. The cap belongs where the number is decided, so the two cannot disagree.
     */
    public static final int MAX_EFFECT_SLOTS = 5;

    public boolean allows(AuraMode mode) {
        return allowedAuraModes.contains(mode);
    }
}

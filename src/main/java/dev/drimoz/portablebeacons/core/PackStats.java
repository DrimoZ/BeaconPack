package dev.drimoz.portablebeacons.core;

import java.util.Set;

/**
 * A pack tier's stats after its augments have been applied. Produced by {@link PackResolver} and
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
public record PackStats(
        int effectSlots,
        int augmentSlots,
        double range,
        int fuelCapacity,
        int maxAmplifier,
        double fuelMultiplier,
        Set<AuraMode> allowedAuraModes,
        boolean hideParticles,
        boolean hideIcon
) {
    public boolean allows(AuraMode mode) {
        return allowedAuraModes.contains(mode);
    }
}

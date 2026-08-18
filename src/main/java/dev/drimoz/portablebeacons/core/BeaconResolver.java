package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * All of the mod's arithmetic, with no dependency on data components, packets or rendering.
 *
 * <p>Keeping this layer pure is what makes the numbers testable in plain JUnit, and what keeps a
 * 1.20.1 backport down to rewriting the serialization rather than the behaviour.
 */
public final class BeaconResolver {

    /** How definitions are looked up, so this class never touches a registry directly. */
    @FunctionalInterface
    public interface Lookup<T> {
        Optional<T> get(ResourceKey<T> key);

        static <T> Lookup<T> of(Function<ResourceKey<T>, Optional<T>> fn) {
            return fn::apply;
        }
    }

    /** Applies every augment to a tier's base stats. Unknown augment keys are ignored. */
    public static BeaconStats resolve(BeaconTierDef tier,
                                    List<AugmentInstance> augments,
                                    Lookup<AugmentDef> augmentLookup) {
        int effectSlots = tier.effectSlots();
        int augmentSlots = tier.augmentSlots();
        double range = tier.baseRange();
        double capacityMultiplier = 1.0;
        int maxAmplifier = tier.maxAmplifier();
        double fuelMultiplier = 1.0;
        int auraTierBonus = 0;
        int concealment = 0;

        for (AugmentInstance instance : dedupeByType(augments)) {
            Optional<AugmentDef> maybeDef = augmentLookup.get(instance.type());
            if (maybeDef.isEmpty()) {
                continue;
            }
            AugmentDef def = maybeDef.get();
            int tierLevel = Math.clamp(instance.tier(), 1, def.maxTier());

            for (AugmentDef.Operation op : def.operations()) {
                double value = op.valueFor(tierLevel);
                switch (op.type()) {
                    case ADD_RANGE -> range += value;
                    case ADD_EFFECT_SLOT -> effectSlots += (int) value;
                    case ADD_AMPLIFIER -> maxAmplifier += (int) value;
                    case MUL_FUEL -> fuelMultiplier *= value;
                    case MUL_CAPACITY -> capacityMultiplier *= value;
                    case UNLOCK_AURA -> auraTierBonus += (int) value;
                    // Highest wins rather than summing: this value names a behaviour, so adding
                    // two of them would be meaningless.
                    case HIDE_EFFECTS -> concealment = Math.max(concealment, (int) value);
                }
            }
        }

        int auraRank = tier.auraRank() + auraTierBonus;
        EnumSet<AuraMode> auraModes = EnumSet.noneOf(AuraMode.class);
        for (AuraMode mode : AuraMode.values()) {
            if (mode.rank() <= auraRank) {
                auraModes.add(mode);
            }
        }

        return new BeaconStats(
                Math.max(0, effectSlots),
                augmentSlots,
                Math.max(0.0, range),
                (int) Math.round(tier.fuelCapacity() * capacityMultiplier),
                Math.clamp(maxAmplifier, 0, 3),
                Math.max(0.0, fuelMultiplier),
                auraModes,
                concealment >= 1,
                concealment >= 2);
    }

    /**
     * Enforces "one augment per type" in the resolver too, not only in the slot's placement rule.
     * The GUI rejects duplicates, but a stack built by a command or a broken datapack must not be
     * able to stack two Range augments.
     */
    private static List<AugmentInstance> dedupeByType(List<AugmentInstance> augments) {
        List<AugmentInstance> kept = new ArrayList<>(augments.size());
        for (AugmentInstance instance : augments) {
            if (kept.stream().noneMatch(other -> other.type().equals(instance.type()))) {
                kept.add(instance);
            }
        }
        return kept;
    }

    /**
     * Total fuel units per second, summed per enabled effect.
     *
     * <p>Range only enters the cost of effects that actually project ({@link AuraMode#isAura()}),
     * which is why the info panel can show a believable per-line cost instead of one opaque total.
     */
    public static double fuelPerSecond(BeaconState state,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup) {
        if (!state.active()) {
            return 0.0;
        }
        double total = 0.0;
        for (EffectSlotConfig slot : state.effects()) {
            total += fuelPerSecond(slot, stats, effectLookup);
        }
        return total * stats.fuelMultiplier();
    }

    /** Per-effect cost, excluding the beacon-wide {@link BeaconStats#fuelMultiplier()}. */
    public static double fuelPerSecond(EffectSlotConfig slot,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup) {
        if (!slot.enabled()) {
            return 0.0;
        }
        Optional<BeaconEffectDef> maybeDef = effectLookup.get(slot.effect());
        if (maybeDef.isEmpty()) {
            return 0.0;
        }
        double base = maybeDef.get().costPerSecond(slot.amplifier(), slot.aura());
        return slot.aura().isAura() ? base * rangeFactor(stats.range()) : base;
    }

    /**
     * Range scales cost mildly rather than linearly: a linear factor made the largest Range augment
     * strictly worse than no augment at all, which is not a choice, just a trap.
     */
    private static double rangeFactor(double range) {
        return 1.0 + range / 64.0;
    }

    /**
     * Drops or clamps anything the current tier and augments no longer permit — trimming excess
     * effect slots, capping amplifiers, and falling back to {@link AuraMode#SELF} for modes that
     * are no longer unlocked. Called whenever an augment is removed, so a downgraded beacon can never
     * keep projecting what it can no longer pay for.
     */
    public static BeaconState sanitize(BeaconState state,
                                     BeaconStats stats,
                                     Lookup<BeaconEffectDef> effectLookup,
                                     BeaconTierDef tier) {
        List<EffectSlotConfig> kept = new ArrayList<>(stats.effectSlots());
        for (EffectSlotConfig slot : state.effects()) {
            if (kept.size() >= stats.effectSlots()) {
                break;
            }
            Optional<BeaconEffectDef> maybeDef = effectLookup.get(slot.effect());
            if (maybeDef.isEmpty()
                    || maybeDef.get().minTier() > tier.level()
                    || !tier.allows(slot.effect())) {
                continue;
            }
            int amplifierCap = Math.min(maybeDef.get().maxAmplifier(), stats.maxAmplifier());
            AuraMode aura = stats.allows(slot.aura()) ? slot.aura() : AuraMode.SELF;
            kept.add(slot.withAmplifier(Math.clamp(slot.amplifier(), 0, amplifierCap))
                    .withAura(aura));
        }
        return state.withEffects(kept)
                .withFuel(Math.min(state.fuel(), stats.fuelCapacity()))
                .withCapacity(stats.fuelCapacity());
    }

    private BeaconResolver() {}
}

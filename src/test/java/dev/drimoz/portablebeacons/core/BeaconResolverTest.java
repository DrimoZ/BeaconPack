package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The point of keeping {@code core} free of components, packets and rendering: the balance maths
 * can be exercised without launching Minecraft.
 */
class BeaconResolverTest {

    private static final BeaconTierDef TIER_4 = new BeaconTierDef(4, 2, 3, 16.0, 12000, 1, 1, List.of());

    private static final ResourceKey<AugmentDef> RANGE = augmentKey("range");
    private static final ResourceKey<AugmentDef> FOCUS = augmentKey("focus");

    private static final AugmentDef RANGE_DEF = new AugmentDef(3, 0, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_RANGE, List.of(4.0, 8.0, 12.0))));
    private static final AugmentDef FOCUS_DEF = new AugmentDef(1, 0, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_EFFECT_SLOT, List.of(1.0))));

    private static final ResourceKey<AugmentDef> ATTUNEMENT = augmentKey("attunement");
    private static final AugmentDef ATTUNEMENT_DEF = new AugmentDef(2, 0, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.UNLOCK_AURA, List.of(1.0, 2.0))));

    private static final BeaconResolver.Lookup<AugmentDef> AUGMENTS = lookup(Map.of(
            RANGE, RANGE_DEF,
            FOCUS, FOCUS_DEF,
            ATTUNEMENT, ATTUNEMENT_DEF));

    @Test
    void augmentsStackAcrossTypes() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 2), new AugmentInstance(FOCUS, 1)),
                AUGMENTS);

        assertEquals(24.0, stats.range());
        assertEquals(3, stats.effectSlots());
    }

    @Test
    void duplicateAugmentTypeIsIgnored() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 3), new AugmentInstance(RANGE, 3)),
                AUGMENTS);

        assertEquals(28.0, stats.range(), "a second Range augment must not stack");
    }

    @Test
    void augmentTierIsClampedToItsDeclaredMaximum() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(FOCUS, 3)),
                AUGMENTS);

        assertEquals(3, stats.effectSlots(), "Focus only exists at tier 1");
    }

    @Test
    void tierOneCannotProject() {
        BeaconStats stats = BeaconResolver.resolve(
                new BeaconTierDef(1, 1, 0, 0.0, 600, 0, 0, List.of()), List.of(), AUGMENTS);

        assertTrue(stats.allows(AuraMode.SELF));
        assertFalse(stats.allows(AuraMode.ALLIES));
    }

    /**
     * The augment shipped doing nothing for two releases: every tier already cleared the threshold
     * it raised, so there was never a mode left for it to unlock. Nothing tested that it did.
     */
    @Test
    void attunementUnlocksModesTheTierDoesNotGrant() {
        BeaconStats unaided = BeaconResolver.resolve(TIER_4, List.of(), AUGMENTS);
        assertTrue(unaided.allows(AuraMode.TEAM), "tier 4 shares with a team on its own");
        assertFalse(unaided.allows(AuraMode.ALLIES), "and needs help to go wider");

        BeaconStats attuned = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(ATTUNEMENT, 1)), AUGMENTS);
        assertTrue(attuned.allows(AuraMode.ALLIES), "Attunement I reaches allies");
        assertFalse(attuned.allows(AuraMode.ALLIES_AND_PETS), "but not their pets yet");

        BeaconStats fully = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(ATTUNEMENT, 2)), AUGMENTS);
        assertTrue(fully.allows(AuraMode.ALLIES_AND_PETS), "Attunement II reaches pets too");
    }

    @Test
    void inactivePackCostsNothing() {
        BeaconState state = new BeaconState(
                List.of(new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.SELF)),
                1000, false, 12000);
        BeaconStats stats = BeaconResolver.resolve(TIER_4, List.of(), AUGMENTS);

        assertEquals(0.0, BeaconResolver.fuelPerSecond(state, stats,
                lookup(Map.of(effectKey("speed"), new BeaconEffectDef(null, 1.0, 1, 1, 2.5)))));
    }

    private static <T> BeaconResolver.Lookup<T> lookup(Map<ResourceKey<T>, T> entries) {
        return key -> Optional.ofNullable(entries.get(key));
    }

    private static ResourceKey<AugmentDef> augmentKey(String path) {
        return ResourceKey.create(BPRegistryKeys.AUGMENT,
                ResourceLocation.fromNamespaceAndPath("portablebeacons", path));
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                ResourceLocation.fromNamespaceAndPath("portablebeacons", path));
    }
}

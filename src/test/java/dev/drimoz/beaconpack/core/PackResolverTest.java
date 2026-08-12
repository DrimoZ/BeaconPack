package dev.drimoz.beaconpack.core;

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
class PackResolverTest {

    private static final PackTierDef TIER_4 = new PackTierDef(4, 2, 3, 16.0, 12000, 1, List.of());

    private static final ResourceKey<AugmentDef> RANGE = augmentKey("range");
    private static final ResourceKey<AugmentDef> FOCUS = augmentKey("focus");

    private static final AugmentDef RANGE_DEF = new AugmentDef(3, 0, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_RANGE, List.of(4.0, 8.0, 12.0))));
    private static final AugmentDef FOCUS_DEF = new AugmentDef(1, 0, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_EFFECT_SLOT, List.of(1.0))));

    private static final PackResolver.Lookup<AugmentDef> AUGMENTS = lookup(Map.of(
            RANGE, RANGE_DEF,
            FOCUS, FOCUS_DEF));

    @Test
    void augmentsStackAcrossTypes() {
        PackStats stats = PackResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 2), new AugmentInstance(FOCUS, 1)),
                AUGMENTS);

        assertEquals(24.0, stats.range());
        assertEquals(3, stats.effectSlots());
    }

    @Test
    void duplicateAugmentTypeIsIgnored() {
        PackStats stats = PackResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 3), new AugmentInstance(RANGE, 3)),
                AUGMENTS);

        assertEquals(28.0, stats.range(), "a second Range augment must not stack");
    }

    @Test
    void augmentTierIsClampedToItsDeclaredMaximum() {
        PackStats stats = PackResolver.resolve(TIER_4,
                List.of(new AugmentInstance(FOCUS, 3)),
                AUGMENTS);

        assertEquals(3, stats.effectSlots(), "Focus only exists at tier 1");
    }

    @Test
    void tierOneCannotProject() {
        PackStats stats = PackResolver.resolve(
                new PackTierDef(1, 1, 0, 0.0, 600, 0, List.of()), List.of(), AUGMENTS);

        assertTrue(stats.allows(AuraMode.SELF));
        assertFalse(stats.allows(AuraMode.ALLIES));
    }

    @Test
    void inactivePackCostsNothing() {
        PackState state = new PackState(
                List.of(new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.SELF)),
                1000, false, 12000);
        PackStats stats = PackResolver.resolve(TIER_4, List.of(), AUGMENTS);

        assertEquals(0.0, PackResolver.fuelPerSecond(state, stats,
                lookup(Map.of(effectKey("speed"), new BeaconEffectDef(null, 1.0, 1, 1, 2.5)))));
    }

    private static <T> PackResolver.Lookup<T> lookup(Map<ResourceKey<T>, T> entries) {
        return key -> Optional.ofNullable(entries.get(key));
    }

    private static ResourceKey<AugmentDef> augmentKey(String path) {
        return ResourceKey.create(BPRegistryKeys.AUGMENT,
                ResourceLocation.fromNamespaceAndPath("beaconpack", path));
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                ResourceLocation.fromNamespaceAndPath("beaconpack", path));
    }
}

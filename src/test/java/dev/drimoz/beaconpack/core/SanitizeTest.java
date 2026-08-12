package dev.drimoz.beaconpack.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code sanitize} is the safety net: it runs after every action and every tick, and is what stops
 * a pack from projecting something it can no longer pay for after an augment is pulled out.
 */
class SanitizeTest {

    private static final ResourceKey<BeaconEffectDef> SPEED = effectKey("speed");
    private static final ResourceKey<BeaconEffectDef> STRENGTH = effectKey("strength");

    private static final BeaconEffectDef SPEED_DEF =
            new BeaconEffectDef(null, 1.0, 1, 1, 2.0);
    private static final BeaconEffectDef STRENGTH_DEF =
            new BeaconEffectDef(null, 2.0, 1, 3, 2.0);

    private static final PackResolver.Lookup<BeaconEffectDef> EFFECTS =
            key -> Optional.ofNullable(Map.of(SPEED, SPEED_DEF, STRENGTH, STRENGTH_DEF).get(key));

    @Test
    void effectsBeyondTheSlotCountAreDropped() {
        PackTierDef tier = tier(4, 1, List.of());
        PackState state = new PackState(List.of(
                slot(SPEED, 0, AuraMode.SELF),
                slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        PackState result = PackResolver.sanitize(state, stats(tier, 1, 1), EFFECTS, tier);

        assertEquals(1, result.effects().size());
    }

    @Test
    void effectsAboveTheTierAreDropped() {
        PackTierDef tier = tier(2, 2, List.of());
        PackState state = new PackState(List.of(slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        assertTrue(PackResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier).effects().isEmpty());
    }

    @Test
    void effectsOutsideTheTierPoolAreDropped() {
        // A themed pack must not keep an effect it was never allowed to project, however it got in.
        PackTierDef tier = tier(4, 2, List.of(SPEED));
        PackState state = new PackState(List.of(slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        assertTrue(PackResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier).effects().isEmpty());
    }

    @Test
    void amplifierIsCappedWhenAmplificationIsRemoved() {
        PackTierDef tier = tier(4, 2, List.of());
        PackState state = new PackState(List.of(slot(SPEED, 1, AuraMode.SELF)), 0, true, 0);

        PackState result = PackResolver.sanitize(state, stats(tier, 2, 0), EFFECTS, tier);

        assertEquals(0, result.effects().getFirst().amplifier());
    }

    @Test
    void auraFallsBackToSelfWhenTheModeIsNoLongerUnlocked() {
        PackTierDef tier = tier(1, 1, List.of());
        PackState state = new PackState(List.of(slot(SPEED, 0, AuraMode.ALLIES)), 0, true, 0);

        PackState result = PackResolver.sanitize(state, stats(tier, 1, 0), EFFECTS, tier);

        assertEquals(AuraMode.SELF, result.effects().getFirst().aura());
    }

    @Test
    void capacityIsCachedForTheItemsFuelBar() {
        PackTierDef tier = tier(4, 2, List.of());
        PackState state = new PackState(List.of(), 99999, true, 0);

        PackState result = PackResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier);

        assertEquals(12000, result.capacity());
        assertEquals(12000, result.fuel(), "fuel above capacity must be clamped");
    }

    private static PackStats stats(PackTierDef tier, int effectSlots, int maxAmplifier) {
        return new PackStats(effectSlots, tier.augmentSlots(), tier.baseRange(), 12000,
                maxAmplifier, 1.0,
                java.util.EnumSet.of(AuraMode.SELF), false, false);
    }

    private static PackTierDef tier(int level, int slots, List<ResourceKey<BeaconEffectDef>> pool) {
        return new PackTierDef(level, slots, 3, 16.0, 12000, 1, pool);
    }

    private static EffectSlotConfig slot(ResourceKey<BeaconEffectDef> effect, int amplifier,
                                         AuraMode aura) {
        return new EffectSlotConfig(effect, amplifier, true, aura);
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                ResourceLocation.fromNamespaceAndPath("beaconpack", path));
    }
}

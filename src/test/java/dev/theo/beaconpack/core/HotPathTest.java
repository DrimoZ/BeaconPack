package dev.theo.beaconpack.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardrails on the two calls that run while the game is running, not benchmarks.
 *
 * <p>The ceilings are deliberately loose - they are there to catch an accidental quadratic loop or
 * an allocation storm introduced later, not to measure this machine. A regression that matters
 * would blow past them by an order of magnitude; ordinary hardware variance will not come close.
 */
class HotPathTest {

    /** One screen redraw resolves once, so a minute of 60 fps GUI is about this many calls. */
    private static final int FRAMES_PER_MINUTE = 3_600;

    /** The ticker charges every 40 ticks: 30 packs on a busy server for five minutes. */
    private static final int TICKS = 30 * 150;

    private static final ResourceKey<BeaconEffectDef> SPEED = effectKey("speed");
    private static final ResourceKey<BeaconEffectDef> HASTE = effectKey("haste");

    private static final PackResolver.Lookup<BeaconEffectDef> EFFECTS = key -> Optional.ofNullable(
            Map.of(SPEED, new BeaconEffectDef(null, 1.0, 1, 1, 2.0),
                    HASTE, new BeaconEffectDef(null, 1.0, 1, 1, 2.0)).get(key));

    private static final PackResolver.Lookup<AugmentDef> AUGMENTS = key -> Optional.of(
            new AugmentDef(3, 0, 0, List.of(
                    new AugmentDef.Operation(AugmentDef.Type.ADD_RANGE, List.of(4.0, 8.0, 12.0)),
                    new AugmentDef.Operation(AugmentDef.Type.MUL_FUEL, List.of(0.75, 0.6, 0.45)))));

    private static final PackTierDef TIER =
            new PackTierDef(4, 2, 3, 16.0, 36000, 1, List.of(SPEED, HASTE));

    private static final List<AugmentInstance> INSTALLED = List.of(
            new AugmentInstance(augmentKey("range"), 3),
            new AugmentInstance(augmentKey("efficiency"), 3),
            new AugmentInstance(augmentKey("capacity"), 3));

    private static final PackState STATE = new PackState(List.of(
            new EffectSlotConfig(SPEED, 1, true, AuraMode.ALLIES),
            new EffectSlotConfig(HASTE, 0, true, AuraMode.SELF)), 20_000, true, 36000);

    @Test
    void resolvingAFullyBuiltPackStaysCheapEnoughToRunEveryFrame() {
        long elapsed = timeOf(() -> {
            for (int i = 0; i < FRAMES_PER_MINUTE; i++) {
                PackResolver.resolve(TIER, INSTALLED, AUGMENTS);
            }
        });
        assertTrue(elapsed < 250, "a minute of GUI redraws took " + elapsed + "ms");
    }

    @Test
    void chargingAndSanitizingStaysCheapEnoughForABusyServer() {
        long elapsed = timeOf(() -> {
            PackStats stats = PackResolver.resolve(TIER, INSTALLED, AUGMENTS);
            for (int i = 0; i < TICKS; i++) {
                PackResolver.fuelPerSecond(STATE, stats, EFFECTS);
                PackResolver.sanitize(STATE, stats, EFFECTS, TIER);
            }
        });
        assertTrue(elapsed < 250, "five minutes of ticking 30 packs took " + elapsed + "ms");
    }

    private static long timeOf(Runnable work) {
        // A warm-up pass, or the first run measures the JIT rather than the code.
        work.run();
        long start = System.nanoTime();
        work.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                ResourceLocation.fromNamespaceAndPath("beaconpack", path));
    }

    private static ResourceKey<AugmentDef> augmentKey(String path) {
        return ResourceKey.create(BPRegistryKeys.AUGMENT,
                ResourceLocation.fromNamespaceAndPath("beaconpack", path));
    }
}

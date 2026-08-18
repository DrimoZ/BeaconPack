package dev.drimoz.portablebeacons.core;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/**
 * Keys of the three datapack registries that drive the whole mod.
 * <p>
 * Lives in {@code core} on purpose: these are plain vanilla types, so the pure logic layer can
 * reference registry entries without pulling in any NeoForge- or version-specific API.
 */
public final class BPRegistryKeys {
    public static final String MOD_ID = "portablebeacons";

    /** {@code data/<ns>/portablebeacons/effect/<name>.json} — which effects a beacon may project. */
    public static final ResourceKey<Registry<BeaconEffectDef>> EFFECT =
            ResourceKey.createRegistryKey(id("effect"));

    /** {@code data/<ns>/portablebeacons/augment/<name>.json} — augment types and their per-tier ops. */
    public static final ResourceKey<Registry<AugmentDef>> AUGMENT =
            ResourceKey.createRegistryKey(id("augment"));

    /** {@code data/<ns>/portablebeacons/tier/<name>.json} — base stats of each beacon tier. */
    public static final ResourceKey<Registry<BeaconTierDef>> TIER =
            ResourceKey.createRegistryKey(id("tier"));

    /** {@code data/<ns>/portablebeacons/fuel/<name>.json} — item to fuel-unit values. */
    public static final ResourceKey<Registry<FuelDef>> FUEL =
            ResourceKey.createRegistryKey(id("fuel"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private BPRegistryKeys() {}
}

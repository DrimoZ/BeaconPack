package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * One entry of the {@code beaconpack:tier} datapack registry: the base stats of a pack item,
 * before augments.
 *
 * <p>Ranges are deliberately far below the vanilla beacon's 20/30/40/50: a beacon that follows you
 * is worth much more than a fixed one at equal range.
 *
 * @param level        1..4, used for ordering and for {@code min_tier} checks
 * @param effectSlots  how many effects may be configured
 * @param augmentSlots how many augment slots are unlocked (of the 3 drawn)
 * @param baseRange    aura radius in blocks; ignored by effects set to {@link AuraMode#SELF}
 * @param fuelCapacity internal buffer in fuel units
 * @param maxAmplifier highest amplifier reachable without an Amplification augment
 */
public record PackTierDef(
        int level,
        int effectSlots,
        int augmentSlots,
        double baseRange,
        int fuelCapacity,
        int maxAmplifier,
        List<ResourceKey<BeaconEffectDef>> effectPool
) {
    public static final Codec<PackTierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4).fieldOf("level").forGetter(PackTierDef::level),
            Codec.intRange(0, 9).fieldOf("effect_slots").forGetter(PackTierDef::effectSlots),
            Codec.intRange(0, 3).fieldOf("augment_slots").forGetter(PackTierDef::augmentSlots),
            Codec.DOUBLE.fieldOf("base_range").forGetter(PackTierDef::baseRange),
            Codec.INT.fieldOf("fuel_capacity").forGetter(PackTierDef::fuelCapacity),
            Codec.intRange(0, 3).optionalFieldOf("max_amplifier", 0)
                    .forGetter(PackTierDef::maxAmplifier),
            ResourceKey.codec(BPRegistryKeys.EFFECT).listOf()
                    .optionalFieldOf("effect_pool", List.of()).forGetter(PackTierDef::effectPool)
    ).apply(i, PackTierDef::new));

    public PackTierDef {
        effectPool = List.copyOf(effectPool);
    }

    /**
     * Whether this pack may project the given effect.
     *
     * <p>An empty pool means "anything the effect registry allows", which is what a datapack gets
     * for free; the shipped tiers all declare one explicitly so that a themed pack cannot quietly
     * inherit the standard list.
     */
    public boolean allows(ResourceKey<BeaconEffectDef> effect) {
        return effectPool.isEmpty() || effectPool.contains(effect);
    }
}

package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/**
 * What an augment item actually is: a pointer into the {@code beaconpack:augment} registry plus a
 * tier. Stored as a data component on the single {@code beaconpack:augment} item.
 */
public record AugmentInstance(ResourceKey<AugmentDef> type, int tier) {

    public static final Codec<AugmentInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(BPRegistryKeys.AUGMENT).fieldOf("type")
                    .forGetter(AugmentInstance::type),
            Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(AugmentInstance::tier)
    ).apply(i, AugmentInstance::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentInstance> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(BPRegistryKeys.AUGMENT), AugmentInstance::type,
                    ByteBufCodecs.VAR_INT, AugmentInstance::tier,
                    AugmentInstance::new);
}

package dev.theo.beaconpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Everything a pack item remembers, held in the single {@code beaconpack:pack} data component.
 *
 * <p>Slot contents (augments + fuel) are deliberately NOT here: they live in the vanilla
 * {@code minecraft:container} component so NeoForge's item-backed handler can write straight back
 * into the stack.
 *
 * <p>This record is the one place a 1.20.1 backport has to be rewritten — everything downstream
 * consumes it, nothing downstream knows how it is stored.
 *
 * @param effects configured effect slots, ordered; may be shorter than the tier's slot count
 * @param fuel    remaining fuel units in the internal buffer
 * @param active  master switch; false means nothing is applied and nothing is consumed
 */
public record PackState(List<EffectSlotConfig> effects, int fuel, boolean active, int capacity) {

    public static final PackState EMPTY = new PackState(List.of(), 0, false, 0);

    public static final Codec<PackState> CODEC = RecordCodecBuilder.create(i -> i.group(
            EffectSlotConfig.CODEC.listOf().optionalFieldOf("effects", List.of())
                    .forGetter(PackState::effects),
            Codec.INT.optionalFieldOf("fuel", 0).forGetter(PackState::fuel),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(PackState::active),
            Codec.INT.optionalFieldOf("capacity", 0).forGetter(PackState::capacity)
    ).apply(i, PackState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackState> STREAM_CODEC =
            StreamCodec.composite(
                    EffectSlotConfig.STREAM_CODEC.apply(ByteBufCodecs.list()), PackState::effects,
                    ByteBufCodecs.VAR_INT, PackState::fuel,
                    ByteBufCodecs.BOOL, PackState::active,
                    ByteBufCodecs.VAR_INT, PackState::capacity,
                    PackState::new);

    public PackState {
        effects = List.copyOf(effects);
    }

    public PackState withEffects(List<EffectSlotConfig> newEffects) {
        return new PackState(newEffects, fuel, active, capacity);
    }

    public PackState withFuel(int newFuel) {
        return new PackState(effects, Math.max(0, newFuel), active, capacity);
    }

    public PackState withActive(boolean newActive) {
        return new PackState(effects, fuel, newActive, capacity);
    }

    /**
     * Cached so the item can draw its own fuel bar.
     *
     * <p>{@code getBarWidth} gets no registry access, and without the capacity the bar had nothing
     * to be a fraction of - it fell back to durability and showed a full bar on an empty pack.
     * Refreshed by {@link PackResolver#sanitize} on every action and every tick.
     */
    public PackState withCapacity(int newCapacity) {
        return new PackState(effects, fuel, active, newCapacity);
    }

    public double fillRatio() {
        return capacity <= 0 ? 0.0 : Math.min(1.0, fuel / (double) capacity);
    }
}

package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * One entry of the {@code portablebeacons:augment} datapack registry.
 * <p>
 * A single registered item ({@code portablebeacons:augment}) carries a reference to one of these plus a
 * tier, so a datapack can introduce a brand new augment without any code — which would be
 * impossible if each augment were its own registered item.
 *
 * <pre>{@code
 * {
 *   "max_tier": 3,
 *   "color": 5636095,
 *   "operations": [
 *     { "type": "add_range", "values": [4.0, 8.0, 12.0] }
 *   ]
 * }
 * }</pre>
 *
 * @param maxTier    highest tier this augment exists in (1..3)
 * @param color      tint applied to the augment texture; alpha is forced opaque at render time
 * @param modelData  selects a model override, so each augment can have its own glyph. 0 falls back
 *                   to the generic gem, which is what a datapack-added augment gets for free
 * @param operations modifiers applied to the pack's resolved stats
 */
public record AugmentDef(int maxTier, int color, int modelData, List<Operation> operations) {

    public static final Codec<AugmentDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 3).optionalFieldOf("max_tier", 3).forGetter(AugmentDef::maxTier),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(AugmentDef::color),
            Codec.INT.optionalFieldOf("model_data", 0).forGetter(AugmentDef::modelData),
            Operation.CODEC.listOf().fieldOf("operations").forGetter(AugmentDef::operations)
    ).apply(i, AugmentDef::new));

    /**
     * A modifier with one value per augment tier. Indexing by tier rather than defining three
     * separate entries keeps "Range I/II/III" a single JSON file.
     *
     * @param values value for tier 1, 2, 3 — must hold at least {@code maxTier} entries
     */
    public record Operation(Type type, List<Double> values) {
        public static final Codec<Operation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Type.CODEC.fieldOf("type").forGetter(Operation::type),
                Codec.DOUBLE.listOf().fieldOf("values").forGetter(Operation::values)
        ).apply(i, Operation::new));

        /** Value for a 1-based augment tier, clamped to what the JSON actually declares. */
        public double valueFor(int tier) {
            if (values.isEmpty()) {
                return 0.0;
            }
            return values.get(Math.clamp(tier - 1, 0, values.size() - 1));
        }
    }

    public enum Type implements StringRepresentable {
        ADD_RANGE("add_range"),
        ADD_EFFECT_SLOT("add_effect_slot"),
        ADD_AMPLIFIER("add_amplifier"),
        MUL_FUEL("mul_fuel"),
        MUL_CAPACITY("mul_capacity"),
        /** Unlocks aura modes whose {@code minTier} the pack alone would not satisfy. */
        UNLOCK_AURA("unlock_aura"),
        /**
         * Hides the swirl at 1, and the status icon as well at 2.
         *
         * <p>The first operation that is not an amount: the value picks a behaviour rather than
         * scaling one, which is why the resolver reads it as a threshold.
         */
        HIDE_EFFECTS("hide_effects");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}

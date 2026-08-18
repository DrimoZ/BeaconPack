package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * One entry of the {@code portablebeacons:fuel} registry: what an item is worth when burned.
 *
 * <p>A registry rather than a tag on its own, because a tag can only say "this is fuel" — it cannot
 * carry the per-item value, and hardcoding the values would undo the point of making the rest
 * data-driven. An entry may still be <em>keyed</em> by a tag, which is how one line covers every
 * modded metal that follows the convention:
 *
 * <pre>{@code
 * { "item": "minecraft:iron_ingot", "units": 300 }
 * { "tag": "c:ingots/steel", "units": 450 }
 * }</pre>
 *
 * @param item  the consumed item, if this entry names one
 * @param tag   the tag of consumed items, if this entry names one instead
 * @param units fuel units any of them yields
 */
public record FuelDef(Optional<Holder<Item>> item, Optional<TagKey<Item>> tag, int units) {

    public static final Codec<FuelDef> CODEC = RecordCodecBuilder.<FuelDef>create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("item")
                    .forGetter(FuelDef::item),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(FuelDef::tag),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("units").forGetter(FuelDef::units)
    ).apply(i, FuelDef::new)).validate(FuelDef::exactlyOneSource);

    /**
     * Rejects an entry naming both or neither.
     *
     * <p>Enforced in the codec rather than at lookup time so a malformed entry is reported when the
     * file is read, with the file's name, instead of silently never matching anything.
     */
    private static DataResult<FuelDef> exactlyOneSource(FuelDef def) {
        if (def.item().isPresent() == def.tag().isPresent()) {
            return DataResult.error(() ->
                    "a fuel entry needs exactly one of \"item\" or \"tag\"");
        }
        return DataResult.success(def);
    }

    /** Whether this entry prices the given item. */
    public boolean matches(Item candidate) {
        if (item.isPresent()) {
            return item.get().value() == candidate;
        }
        // wrapAsHolder rather than Item#builtInRegistryHolder, which vanilla deprecates.
        return tag.isPresent() && BuiltInRegistries.ITEM.wrapAsHolder(candidate).is(tag.get());
    }
}

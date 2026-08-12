package dev.drimoz.beaconpack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * One entry of the {@code beaconpack:fuel} registry: what an item is worth when burned.
 *
 * <p>A registry rather than a tag, because a tag can only say "this is fuel" — it cannot carry the
 * per-item value, and hardcoding the values would undo the point of making the rest data-driven.
 *
 * @param item  the consumed item
 * @param units fuel units it yields
 */
public record FuelDef(Holder<Item> item, int units) {

    public static final Codec<FuelDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(FuelDef::item),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("units").forGetter(FuelDef::units)
    ).apply(i, FuelDef::new));
}

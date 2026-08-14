package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class BPTags {

    /**
     * Every pack item, including any a datapack adds.
     *
     * <p>Exists so the Curios binding and anything downstream can name "a pack" once rather than
     * listing seven items and going stale the moment an eighth appears.
     */
    public static final TagKey<Item> PACKS =
            TagKey.create(Registries.ITEM, BPRegistryKeys.id("packs"));

    private BPTags() {}
}

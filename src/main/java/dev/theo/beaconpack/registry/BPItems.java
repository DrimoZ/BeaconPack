package dev.theo.beaconpack.registry;

import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.item.AugmentItem;
import dev.theo.beaconpack.item.BeaconPackItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class BPItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(BeaconPack.MOD_ID);

    /**
     * Four distinct items rather than one item plus a tier component: recipes, tags and JEI all
     * stay trivial, and a player can tell two packs apart in a chest at a glance.
     */
    public static final DeferredItem<BeaconPackItem> PACK_I = pack("beacon_pack_i", 1, Rarity.COMMON);
    public static final DeferredItem<BeaconPackItem> PACK_II = pack("beacon_pack_ii", 2, Rarity.UNCOMMON);
    public static final DeferredItem<BeaconPackItem> PACK_III = pack("beacon_pack_iii", 3, Rarity.RARE);
    public static final DeferredItem<BeaconPackItem> PACK_IV = pack("beacon_pack_iv", 4, Rarity.EPIC);

    /**
     * One registered augment item for every augment there will ever be — its identity comes from a
     * component pointing into the datapack registry, so a pack can add new augments without code.
     */
    public static final DeferredItem<AugmentItem> AUGMENT =
            ITEMS.registerItem("augment", props -> new AugmentItem(props.stacksTo(1)));

    public static List<DeferredItem<BeaconPackItem>> packs() {
        return List.of(PACK_I, PACK_II, PACK_III, PACK_IV);
    }

    private static DeferredItem<BeaconPackItem> pack(String name, int level, Rarity rarity) {
        ResourceKey<PackTierDef> tier =
                ResourceKey.create(BPRegistryKeys.TIER, BPRegistryKeys.id("tier_" + level));
        return ITEMS.registerItem(name, props ->
                new BeaconPackItem(props.stacksTo(1).rarity(rarity), tier));
    }

    private BPItems() {}
}

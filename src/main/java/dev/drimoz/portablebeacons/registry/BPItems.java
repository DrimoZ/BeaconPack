package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.PackTierDef;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class BPItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PortableBeacons.MOD_ID);

    /**
     * Four distinct items rather than one item plus a tier component: recipes, tags and JEI all
     * stay trivial, and a player can tell two packs apart in a chest at a glance.
     */
    public static final DeferredItem<PortableBeaconItem> PACK_I =
            pack("beacon_pack_i", "tier_1", Rarity.COMMON);
    public static final DeferredItem<PortableBeaconItem> PACK_II =
            pack("beacon_pack_ii", "tier_2", Rarity.UNCOMMON);
    public static final DeferredItem<PortableBeaconItem> PACK_III =
            pack("beacon_pack_iii", "tier_3", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> PACK_IV =
            pack("beacon_pack_iv", "tier_4", Rarity.EPIC);

    /**
     * Themed packs: same machinery, different effect pool.
     *
     * <p>They exist entirely because a tier entry can declare which effects it accepts, so a
     * specialist pack needs no code of its own - and a datapack can add more the same way. Each
     * trades the standard list for effects the beacon never offered, which is what makes carrying
     * one instead of a tier IV a real choice rather than a downgrade.
     */
    public static final DeferredItem<PortableBeaconItem> PACK_NETHER =
            pack("nether_pack", "nether", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> PACK_END =
            pack("end_pack", "end", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> PACK_TIDAL =
            pack("tidal_pack", "tidal", Rarity.RARE);

    /**
     * One registered augment item for every augment there will ever be — its identity comes from a
     * component pointing into the datapack registry, so a pack can add new augments without code.
     */
    public static final DeferredItem<AugmentItem> AUGMENT =
            ITEMS.registerItem("augment", props -> new AugmentItem(props.stacksTo(1)));

    public static List<DeferredItem<PortableBeaconItem>> packs() {
        return List.of(PACK_I, PACK_II, PACK_III, PACK_IV, PACK_NETHER, PACK_END, PACK_TIDAL);
    }

    private static DeferredItem<PortableBeaconItem> pack(String name, String tierPath, Rarity rarity) {
        ResourceKey<PackTierDef> tier =
                ResourceKey.create(BPRegistryKeys.TIER, BPRegistryKeys.id(tierPath));
        return ITEMS.registerItem(name, props ->
                new PortableBeaconItem(props.stacksTo(1).rarity(rarity), tier));
    }

    private BPItems() {}
}

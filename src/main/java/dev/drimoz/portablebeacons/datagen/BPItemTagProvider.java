package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

/**
 * The beacon item tag, filled from {@link BPItems#packs()} so it cannot fall behind the registry.
 */
public class BPItemTagProvider extends ItemTagsProvider {

    public BPItemTagProvider(PackOutput output,
                             CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PortableBeacons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var beacons = tag(BPTags.PACKS);
        BPItems.packs().forEach(beacon -> beacons.add(beacon.get()));
    }
}

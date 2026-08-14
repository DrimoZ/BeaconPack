package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * The pack item tag, filled from {@link BPItems#packs()} so it cannot fall behind the registry.
 *
 * <p>The block-tag lookup is empty on purpose: the vanilla provider takes one so it can copy block
 * tags into item tags, and this mod has no blocks to copy from.
 */
public class BPItemTagProvider extends ItemTagsProvider {

    public BPItemTagProvider(PackOutput output,
                             CompletableFuture<HolderLookup.Provider> registries,
                             ExistingFileHelper helper) {
        super(output, registries, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                PortableBeacons.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var packs = tag(BPTags.PACKS);
        BPItems.packs().forEach(pack -> packs.add(pack.get()));
    }
}

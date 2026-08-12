package dev.drimoz.beaconpack.datagen;

import dev.drimoz.beaconpack.BeaconPack;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.registry.BPItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item models: twelve files that differ only by a texture path, plus the augment's override table.
 *
 * <p>The overrides are the reason this is worth generating rather than typing. Every built-in
 * augment needs its {@code model_data} in its registry entry and the matching predicate here, and
 * the two silently drifting apart shows up as an augment rendering as the wrong glyph.
 */
public class BPItemModelProvider extends ItemModelProvider {

    /** Order defines the model_data values, which the registry entries must match. */
    private static final String[] AUGMENTS = {
            "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion"
    };

    public BPItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, BeaconPack.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        BPItems.packs().forEach(pack -> generated(name(pack.getId())));

        ItemModelBuilder augment = generated("augment");
        for (int i = 0; i < AUGMENTS.length; i++) {
            String glyph = "augment_" + AUGMENTS[i];
            generated(glyph);
            augment.override()
                    .predicate(BPRegistryKeys.id("augment_type"), i + 1)
                    .model(getExistingFile(modLoc("item/" + glyph)))
                    .end();
        }
    }

    private ItemModelBuilder generated(String name) {
        return withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }

    private static String name(ResourceLocation id) {
        return id.getPath();
    }
}

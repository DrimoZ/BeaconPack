package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Item models: twelve flat models, plus the augment's glyph table.
 *
 * <p>The table is the reason this is worth generating rather than typing. It selects on the
 * augment's registry key — see {@link AugmentLook} — so adding an augment here means naming it
 * once, not keeping an integer in step across two files.
 */
public class BPItemModelProvider extends ModelProvider {

    /** The built-in augments, by registry name. Order is presentational only. */
    private static final String[] AUGMENTS = {
            "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion"
    };

    public BPItemModelProvider(PackOutput output) {
        super(output, PortableBeacons.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BPItems.beacons().forEach(beacon ->
                itemModels.generateFlatItem(beacon.get(), ModelTemplates.FLAT_ITEM));

        List<SelectItemModel.SwitchCase<ResourceKey<AugmentDef>>> cases =
                new ArrayList<>(AUGMENTS.length);
        for (String name : AUGMENTS) {
            // Each glyph is a model in its own right, tinted from the registry entry so a
            // datapack augment can reuse one without shipping a texture.
            ItemModel.Unbaked glyph = ItemModelUtils.tintedModel(
                    flatModel(itemModels, "augment_" + name), AugmentLook.Tint.INSTANCE);
            cases.add(ItemModelUtils.when(
                    ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id(name)), glyph));
        }

        // The fallback is the untyped augment, which is what a stack with no component - or one
        // naming an augment this beacon does not define - should look like.
        itemModels.itemModelOutput.accept(BPItems.AUGMENT.get(), ItemModelUtils.select(
                AugmentLook.TypeProperty.INSTANCE,
                ItemModelUtils.tintedModel(flatModel(itemModels, "augment"),
                        AugmentLook.Tint.INSTANCE),
                cases));
    }

    /**
     * A flat model under {@code models/item/<name>}, returning where it was written.
     *
     * <p>Not {@code generateFlatItem}: these are named models the select table points at, not
     * models for registered items. Referencing one without writing it is a missing-texture
     * checkerboard and nothing else — nothing checks that the two agree.
     */
    private static Identifier flatModel(ItemModelGenerators itemModels, String name) {
        Identifier id = BPRegistryKeys.id("item/" + name);
        return ModelTemplates.FLAT_ITEM.create(
                id,
                new TextureMapping().put(TextureSlot.LAYER0, new Material(id)),
                itemModels.modelOutput);
    }
}

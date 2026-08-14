package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.Criterion;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

/**
 * A short advancement branch: get a pack, kit it out, specialise.
 *
 * <p>Four entries, not a tree. The mod has one item family and no progression of its own beyond the
 * crafting ladder, so a long chain would be padding - and the recipe-unlock advancements that ship
 * alongside these are already hidden ones the player never sees.
 *
 * <p>Every trigger is "you have the item", which is the honest condition: the packs are craftable,
 * but a modpack may hand them out, and an advancement that only fires on crafting would silently
 * never trigger there.
 */
public class BPAdvancementProvider implements AdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                         ExistingFileHelper helper) {

        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        BPItems.PACK_I.get(),
                        title("root"),
                        description("root"),
                        // Reuses a vanilla background rather than shipping one: a bespoke tab
                        // texture for four advancements is not worth the kilobytes.
                        ResourceLocation.withDefaultNamespace("textures/block/deepslate_bricks.png"),
                        AdvancementType.TASK,
                        true, true, false)
                .addCriterion("has_pack", hasTaggedPack())
                .save(saver, id("root"), helper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.AUGMENT.get(), title("augmented"), description("augmented"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_augment", has(BPItems.AUGMENT.get()))
                .save(saver, id("augmented"), helper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.PACK_IV.get(), title("tier_four"), description("tier_four"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("has_pack_iv", has(BPItems.PACK_IV.get()))
                .save(saver, id("tier_four"), helper);

        // Any one of the three, because they are alternatives rather than a set to collect.
        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.PACK_TIDAL.get(), title("themed"), description("themed"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_cinder", has(BPItems.PACK_NETHER.get()))
                .addCriterion("has_void", has(BPItems.PACK_END.get()))
                .addCriterion("has_tidal", has(BPItems.PACK_TIDAL.get()))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(saver, id("themed"), helper);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> hasTaggedPack() {
        return InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(BPTags.PACKS));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private static Component title(String name) {
        return Component.translatable("advancement." + PortableBeacons.MOD_ID + "." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancement." + PortableBeacons.MOD_ID + "." + name + ".description");
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(PortableBeacons.MOD_ID, name);
    }
}

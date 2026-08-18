package dev.drimoz.portablebeacons.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import java.util.Map;

/**
 * A shaped recipe whose result carries data components.
 *
 * <p>Vanilla's {@code ShapedRecipeBuilder} builds its result from an {@link ItemLike}, so it cannot
 * express "one augment item, of this type, at this tier" - and thirteen of this mod's twenty
 * recipes are exactly that. Without this, datagen would have covered half of them and left the
 * rest as hand-written JSON, which is worse than either extreme: two places to look.
 */
final class ComponentShapedRecipe {

    static void save(RecipeOutput output, HolderLookup.Provider registries, Identifier id,
                     ItemStack result, ItemLike unlockedBy,
                     Map<Character, Ingredient> key, String... pattern) {
        // A recipe result is an ItemStackTemplate now - immutable, and resolvable before the
        // registries are up, which is why the split happened. The components carry across intact.
        ShapedRecipe recipe = new ShapedRecipe(
                new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                ShapedRecipePattern.of(key, pattern),
                new ItemStackTemplate(result.getItem(), result.getCount(),
                        result.getComponentsPatch()));

        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, id);

        // The same unlock advancement vanilla's builder writes: without one the recipe never shows
        // up in the recipe book, which is the only way to find it without JEI.
        Advancement.Builder advancement = Advancement.Builder.recipeAdvancement()
                .addCriterion("has_material", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item()
                                .of(registries.lookupOrThrow(Registries.ITEM), unlockedBy)))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeKey))
                .rewards(AdvancementRewards.Builder.recipe(recipeKey))
                .requirements(AdvancementRequirements.Strategy.OR);

        output.accept(recipeKey, recipe,
                advancement.build(id.withPrefix("recipes/misc/")));
    }

    private ComponentShapedRecipe() {}
}

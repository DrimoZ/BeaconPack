package dev.drimoz.portablebeacons.compat.emi;

import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

/**
 * EMI integration, matching the JEI one feature for feature.
 *
 * <p>Only loaded by EMI itself - nothing else references this package and EMI is compiled against
 * but never required, the same arrangement that keeps the JEI plugin optional.
 */
@EmiEntrypoint
public class PortableBeaconsEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory FUEL = new EmiRecipeCategory(
            BPRegistryKeys.id("fuel"), EmiStack.of(BPItems.PACK_II.get()));

    @Override
    public void register(EmiRegistry registry) {
        // The whole augment problem in one line: EMI compares data components, so the fifteen
        // augments stop collapsing into a single entry. JEI needed an interpreter for the same
        // thing because it compares by item id unless told otherwise.
        registry.setDefaultComparison(BPItems.AUGMENT.get(), c -> Comparison.compareComponents());

        registry.addCategory(FUEL);
        // Every pack is a workstation for it, so the category is reachable from the item a player
        // is actually holding when the question occurs to them.
        BPItems.packs().forEach(pack -> registry.addWorkstation(FUEL, EmiStack.of(pack.get())));

        addFuels(registry);
    }

    /**
     * Rows come from the {@code portablebeacons:fuel} registry, so a datapack's own fuels list
     * themselves. A tag-keyed entry becomes one row whose slot cycles through the tag.
     */
    private static void addFuels(EmiRegistry registry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        RegistryAccess access = minecraft.level.registryAccess();

        for (var entry : access.registryOrThrow(BPRegistryKeys.FUEL).entrySet()) {
            FuelDef def = entry.getValue();
            EmiIngredient input = def.item()
                    .<EmiIngredient>map(holder -> EmiStack.of(holder.value()))
                    .or(() -> def.tag().map(EmiIngredient::of))
                    .orElse(null);
            if (input == null || input.isEmpty()) {
                continue;
            }
            // Leading slash: EMI reserves plain ids for recipes it can find in the recipe manager,
            // and warns on screen about any it cannot. These are built from a registry, not from a
            // JSON recipe, so they are synthetic and have to say so.
            ResourceLocation id = entry.getKey().location().withPrefix("/fuel/");
            // One unit is one second of one basic effect kept to yourself - the scale the rest of
            // the mod is written in, so the two agree by construction.
            registry.addRecipe(new FuelEmiRecipe(id, input, def.units(), def.units()));
        }
    }
}

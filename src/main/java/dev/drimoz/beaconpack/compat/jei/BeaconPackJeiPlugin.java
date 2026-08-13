package dev.drimoz.beaconpack.compat.jei;

import dev.drimoz.beaconpack.core.AugmentInstance;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.core.FuelDef;
import dev.drimoz.beaconpack.item.AugmentItem;
import dev.drimoz.beaconpack.registry.BPItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JEI integration.
 *
 * <p>Nothing outside this package references it and JEI is compiled against but never required, so
 * this class is only ever loaded by JEI itself - which is what keeps JEI optional without a load
 * check of the kind the Curios integration needs.
 */
@JeiPlugin
public class BeaconPackJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = BPRegistryKeys.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * Tells JEI that two augments are different items.
     *
     * <p>Without this, all fifteen augments collapse into one entry: they share a single registered
     * item and differ only by a data component, which JEI has no way to guess is meaningful. The
     * visible symptom is fourteen missing items and fifteen recipes that all produce an
     * indistinguishable "Augment".
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(BPItems.AUGMENT.get(),
                (IIngredientSubtypeInterpreter<ItemStack>) (stack, context) -> {
                    AugmentInstance instance = AugmentItem.instanceOf(stack);
                    if (instance == null) {
                        return IIngredientSubtypeInterpreter.NONE;
                    }
                    return instance.type().location() + "@" + instance.tier();
                });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FuelCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FuelCategory.TYPE, fuelEntries());
    }

    /**
     * Read from the datapack registry rather than hardcoded, so a pack's own fuels appear here on
     * their own - the same reason the creative tab enumerates augments instead of listing them.
     */
    private static List<FuelEntry> fuelEntries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // JEI builds its lists on world load, so this should not happen; returning empty beats
            // throwing and taking the whole plugin down with it.
            return List.of();
        }
        RegistryAccess access = minecraft.level.registryAccess();

        List<FuelEntry> entries = new ArrayList<>();
        for (FuelDef def : access.registryOrThrow(BPRegistryKeys.FUEL)) {
            // One unit is one second of one basic effect kept to yourself - the scale the whole
            // GUI is written in, so the two agree by construction.
            entries.add(new FuelEntry(new ItemStack(def.item().value()), def.units(), def.units()));
        }
        entries.sort(Comparator.comparingInt(FuelEntry::units));
        return entries;
    }
}

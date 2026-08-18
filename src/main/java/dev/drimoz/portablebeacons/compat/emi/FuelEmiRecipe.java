package dev.drimoz.portablebeacons.compat.emi;

import dev.drimoz.portablebeacons.core.Durations;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One row of EMI's fuel category: what burns, and how long it runs a pack for.
 *
 * <p>Deliberately the same layout and the same wording as the JEI category. A player who switches
 * viewers should not have to relearn the figure, and both read it from the same registry.
 */
public class FuelEmiRecipe implements EmiRecipe {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 26;

    private final ResourceLocation id;
    private final EmiIngredient input;
    private final int units;
    private final int seconds;

    public FuelEmiRecipe(ResourceLocation id, EmiIngredient input, int units, int seconds) {
        this.id = id;
        this.input = input;
        this.units = units;
        this.seconds = seconds;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PortableBeaconsEmiPlugin.FUEL;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(input);
    }

    @Override
    public List<EmiStack> getOutputs() {
        // Burning fuel produces runtime, not an item. An empty output is honest; inventing one
        // would put a fake item in every search for it.
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(input, 0, 4);
        // Time first and in white: it is the answer to the question people are asking. The unit
        // count is the same fact in the mod's own currency, so it is there but subdued.
        widgets.addText(Component.translatable("portablebeacons.viewer.fuel_runtime",
                Durations.format(seconds)), 24, 5, 0xFFFFFF, false);
        widgets.addText(Component.translatable("portablebeacons.viewer.fuel_units", units)
                .withStyle(ChatFormatting.GRAY), 24, 16, 0xA0A0A0, false);
    }
}

package dev.drimoz.beaconpack.compat.jei;

import dev.drimoz.beaconpack.core.Durations;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.registry.BPItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What an item is worth as pack fuel.
 *
 * <p>This exists because it is the one number the mod never shows anywhere else: nothing in game
 * says an iron ingot is five minutes and a netherite ingot is eight hours, so the only way to find
 * out was to burn one and watch.
 *
 * <p>Rows come from the {@code beaconpack:fuel} registry, so a pack that adds its own fuels gets
 * them listed here with no further work.
 */
public class FuelCategory implements IRecipeCategory<FuelEntry> {

    public static final RecipeType<FuelEntry> TYPE =
            RecipeType.create(BPRegistryKeys.id("fuel").getNamespace(), "fuel", FuelEntry.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 26;

    private final IDrawable icon;
    private final IDrawable slot;

    public FuelCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(BPItems.PACK_II.get()));
        this.slot = helper.getSlotDrawable();
    }

    @Override
    public RecipeType<FuelEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("beaconpack.viewer.fuel");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FuelEntry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5)
                .addIngredients(VanillaTypes.ITEM_STACK, entry.items());
    }

    @Override
    public void draw(FuelEntry entry, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        slot.draw(graphics, 0, 4);

        var font = Minecraft.getInstance().font;
        // Time first and in white: it is the answer to the question people are asking. The unit
        // count is the same fact in the mod's own currency, so it is there but subdued.
        graphics.drawString(font,
                Component.translatable("beaconpack.viewer.fuel_runtime",
                        Durations.format(entry.seconds())),
                24, 4, 0xFFFFFF, false);
        graphics.drawString(font,
                Component.translatable("beaconpack.viewer.fuel_units", entry.units())
                        .withStyle(ChatFormatting.GRAY),
                24, 15, 0xA0A0A0, false);
    }
}

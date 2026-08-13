package dev.drimoz.beaconpack.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One row of the fuel category: what burns, what it is worth, and how long that lasts.
 *
 * @param items   the burnable items. More than one when the entry is keyed by a tag, in which case
 *                the slot cycles through them rather than the category repeating the same figure
 * @param units   their value in fuel units
 * @param seconds runtime that buys for one basic effect kept to yourself, which is what a unit means
 */
public record FuelEntry(List<ItemStack> items, int units, int seconds) {

    public FuelEntry {
        items = List.copyOf(items);
    }
}

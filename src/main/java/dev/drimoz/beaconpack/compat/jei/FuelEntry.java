package dev.drimoz.beaconpack.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * One row of the fuel category: an item, what it is worth, and how long that actually lasts.
 *
 * @param item    the burnable item
 * @param units   its value in fuel units
 * @param seconds runtime it buys for one basic effect kept to yourself, which is what a unit means
 */
public record FuelEntry(ItemStack item, int units, int seconds) {}

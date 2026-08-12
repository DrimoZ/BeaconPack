package dev.theo.beaconpack.compat;

import dev.theo.beaconpack.item.BeaconPackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional Curios support: a pack worn in a curio slot works exactly like one carried in the
 * inventory.
 *
 * <p>Curios is compiled against but never required. Every call into its API sits behind
 * {@link #loaded()} and inside a nested class, so the classes are only ever resolved when the mod
 * is actually present - referencing them from this class directly would fail to link without it.
 *
 * <p>No slot type is declared and no tag is shipped: which slot a pack may go in is the pack
 * author's or the player's business, and forcing one would fight whatever slot layout their pack
 * already uses.
 */
public final class CuriosCompat {

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    public static boolean loaded() {
        return LOADED;
    }

    /** The first active pack worn as a curio, or empty. */
    public static ItemStack findActivePack(Player player) {
        return LOADED ? Inner.findActivePack(player) : ItemStack.EMPTY;
    }

    private static final class Inner {
        static ItemStack findActivePack(Player player) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .flatMap(inventory -> inventory.findFirstCurio(stack ->
                            stack.getItem() instanceof BeaconPackItem
                                    && BeaconPackItem.stateOf(stack).active()))
                    .map(found -> found.stack())
                    .orElse(ItemStack.EMPTY);
        }

        private Inner() {}
    }

    private CuriosCompat() {}
}

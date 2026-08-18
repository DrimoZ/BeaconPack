package dev.drimoz.portablebeacons.compat;

import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional Curios support: a beacon worn in a curio slot works exactly like one carried in the
 * inventory.
 *
 * <p>Curios is compiled against but never required. Every call into its API sits behind
 * {@link #loaded()} and inside a nested class, so the classes are only ever resolved when the mod
 * is actually present - referencing them from this class directly would fail to link without it.
 *
 * <p>Beacons bind to Curios' {@code charm} slot through a shipped tag rather than being left for the
 * beacon author to wire up. Declaring nothing was the tidier position, but it meant a player who
 * installed both mods found the integration simply did not work, with no way to tell that a
 * config file was missing. The binding is additive - {@code replace: false} on both files - so a
 * datapack can still move beacons to another slot.
 */
public final class CuriosCompat {

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    public static boolean loaded() {
        return LOADED;
    }

    /** The first active beacon worn as a curio, or empty. */
    public static ItemStack findActiveBeacon(Player player) {
        return LOADED ? Inner.find(player, true) : ItemStack.EMPTY;
    }

    /**
     * The first beacon worn as a curio whether it is switched on or not, or empty.
     *
     * <p>Separate from {@link #findActiveBeacon} because the two answer different questions: the
     * ticker wants a beacon that is running, while opening the screen must find the beacon precisely
     * when it is switched off - that is usually why the player is opening it.
     */
    public static ItemStack findBeacon(Player player) {
        return LOADED ? Inner.find(player, false) : ItemStack.EMPTY;
    }

    private static final class Inner {
        static ItemStack find(Player player, boolean mustBeActive) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .flatMap(inv -> inv.findFirstCurio(stack ->
                            stack.getItem() instanceof PortableBeaconItem
                                    && (!mustBeActive || PortableBeaconItem.stateOf(stack).active())))
                    .map(found -> found.stack())
                    .orElse(ItemStack.EMPTY);
        }

        private Inner() {}
    }

    private CuriosCompat() {}
}

package dev.drimoz.portablebeacons.menu;

import dev.drimoz.portablebeacons.compat.CuriosCompat;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Opens a pack's menu from an inventory slot index.
 *
 * <p>The index is always resolved server-side, including for the key binding: a client-supplied
 * slot index would let a crafted packet open a menu over an arbitrary stack.
 */
public final class PackMenuOpener {

    /** Only the 36 slots the menu actually draws. A pack in the offhand still works, but the menu
     *  has no slot to freeze for it, and an unfrozen source slot is how contents get duplicated. */
    public static final int MAX_OPENABLE_SLOT = 36;

    public static boolean open(ServerPlayer player, int slotIndex) {
        if (slotIndex != PortableBeaconMenu.CURIO_SLOT
                && (slotIndex < 0 || slotIndex >= MAX_OPENABLE_SLOT)) {
            return false;
        }
        ItemStack stack = slotIndex == PortableBeaconMenu.CURIO_SLOT
                ? CuriosCompat.findPack(player)
                : player.getInventory().getItem(slotIndex);
        if (!(stack.getItem() instanceof PortableBeaconItem)) {
            return false;
        }
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, who) -> new PortableBeaconMenu(id, inventory, slotIndex),
                        stack.getHoverName()),
                buf -> buf.writeVarInt(slotIndex));
        return true;
    }

    /**
     * The pack the key binding should open: the one worn as a curio if there is one, otherwise the
     * first in the inventory, hotbar first.
     *
     * <p>Curios is checked first to match which pack actually runs. Before this the binding only
     * ever looked in the inventory, so a player wearing their only pack pressed the key and nothing
     * happened at all - which reads as the binding being broken rather than as a missed slot.
     */
    public static int findPack(ServerPlayer player) {
        if (!CuriosCompat.findPack(player).isEmpty()) {
            return PortableBeaconMenu.CURIO_SLOT;
        }
        // Inventory indices 0-8 are the hotbar already, so plain ascending order is hotbar first.
        for (int slot = 0; slot < MAX_OPENABLE_SLOT; slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof PortableBeaconItem) {
                return slot;
            }
        }
        return NONE;
    }

    /** Returned by {@link #findPack} when the player has no pack at all. */
    public static final int NONE = -99;

    private PackMenuOpener() {}
}

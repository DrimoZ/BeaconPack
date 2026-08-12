package dev.theo.beaconpack.menu;

import dev.theo.beaconpack.item.BeaconPackItem;
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
        if (slotIndex < 0 || slotIndex >= MAX_OPENABLE_SLOT) {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(slotIndex);
        if (!(stack.getItem() instanceof BeaconPackItem)) {
            return false;
        }
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, who) -> new BeaconPackMenu(id, inventory, slotIndex),
                        Component.translatable(stack.getDescriptionId())),
                buf -> buf.writeVarInt(slotIndex));
        return true;
    }

    /** First pack in the inventory, hotbar first, for the key binding. */
    public static int findPack(ServerPlayer player) {
        // Inventory indices 0-8 are the hotbar already, so plain ascending order is hotbar first.
        for (int slot = 0; slot < MAX_OPENABLE_SLOT; slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof BeaconPackItem) {
                return slot;
            }
        }
        return -1;
    }

    private PackMenuOpener() {}
}

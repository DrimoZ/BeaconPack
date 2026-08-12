package dev.theo.beaconpack.registry;

import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.item.BeaconPackItem;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Backs each pack's augment and fuel slots with vanilla's {@code minecraft:container} component.
 *
 * <p>{@link ComponentItemHandler} writes every change straight back into the stack's component,
 * which is what keeps a pack sitting in a chest, an ender chest or another player's inventory from
 * ever holding a stale copy of its own contents.
 */
@EventBusSubscriber(modid = BeaconPack.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class BPCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        for (DeferredItem<BeaconPackItem> pack : BPItems.packs()) {
            event.registerItem(
                    Capabilities.ItemHandler.ITEM,
                    (stack, context) -> new ComponentItemHandler(
                            stack, DataComponents.CONTAINER, BeaconPackItem.CONTAINER_SIZE),
                    pack.get());
        }
    }

    private BPCapabilities() {}
}

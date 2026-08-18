package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.PortableBeacons;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.tooltip.TooltipAppender;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

/**
 * Where this mod's tooltip lines come from.
 *
 * <p>{@code Item#appendHoverText} is deprecated: tooltips are assembled from registered appenders
 * now rather than from an override on the item.
 *
 * <p>The obvious reading of that is to make the data components implement {@code TooltipProvider},
 * which is what vanilla does — but {@code PackState} and {@code AugmentInstance} live in
 * {@code core}, the layer with no dependency on components, rendering or {@code Component}. An
 * appender is an interface this layer can implement directly, so the text stays where it already
 * was and {@code core} stays what it is.
 *
 * <p>{@code HEAD} because both items open with a line about their own state, and that belongs
 * directly under the name rather than after whatever vanilla has to say.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPTooltips {

    @SubscribeEvent
    public static void register(RegisterTooltipAppendersEvent event) {
        // Appenders run for every stack, so each one identifies its own item first.
        event.registerAppender(TooltipLocation.HEAD,
                (stack, context, display, player, flag, builder) -> {
                    if (stack.getItem() instanceof PortableBeaconItem beacon) {
                        beacon.appendTooltip(stack, context, builder);
                    }
                });
        event.registerAppender(TooltipLocation.HEAD,
                (stack, context, display, player, flag, builder) -> {
                    if (stack.getItem() instanceof AugmentItem) {
                        AugmentItem.appendTooltip(stack, context, builder);
                    }
                });
    }

    private BPTooltips() {}
}

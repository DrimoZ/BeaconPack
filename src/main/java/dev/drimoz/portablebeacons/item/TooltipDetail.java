package dev.drimoz.portablebeacons.item;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Shift-to-expand tooltips.
 *
 * <p>Packs and augments both carry more detail than fits on a hover, and dumping all of it on every
 * item makes chests unreadable. Shift is the convention players already know from the wider modded
 * ecosystem, so it needs no explanation beyond the hint line.
 */
public final class TooltipDetail {

    public static final Component HINT =
            Component.translatable("portablebeacons.tip.hold_shift").withStyle(style -> style
                    .withColor(net.minecraft.ChatFormatting.DARK_GRAY)
                    .withItalic(true));

    public static boolean expanded() {
        return FMLEnvironment.getDist() == Dist.CLIENT && ClientOnly.shiftDown();
    }

    /** Nested so the client-only class is never loaded on a dedicated server. */
    private static final class ClientOnly {
        static boolean shiftDown() {
            return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        }

        private ClientOnly() {}
    }

    private TooltipDetail() {}
}

package dev.drimoz.beaconpack.net;

import dev.drimoz.beaconpack.BeaconPack;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.menu.PackMenuOpener;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Asks the server to open a pack's menu.
 *
 * <p>The slot index is a hint, never trusted: {@link PackMenuOpener#open} re-checks that it is in
 * range and actually holds a pack. {@link #FIND_ANY} lets the key binding say "whichever one I
 * have" without the client having to scan.
 */
public record OpenPackPayload(int slot) implements CustomPacketPayload {

    public static final int FIND_ANY = -1;

    public static final Type<OpenPackPayload> TYPE = new Type<>(BPRegistryKeys.id("open_pack"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, OpenPackPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenPackPayload::slot, OpenPackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = BeaconPack.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Handler {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(TYPE, STREAM_CODEC, (payload, context) ->
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }
                        int slot = payload.slot() == FIND_ANY
                                ? PackMenuOpener.findPack(player)
                                : payload.slot();
                        if (slot >= 0) {
                            PackMenuOpener.open(player, slot);
                        }
                    }));
        }

        private Handler() {}
    }
}

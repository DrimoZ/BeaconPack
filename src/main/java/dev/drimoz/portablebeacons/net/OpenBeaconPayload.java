package dev.drimoz.portablebeacons.net;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.menu.BeaconMenuOpener;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Asks the server to open a beacon's menu.
 *
 * <p>The slot index is a hint, never trusted: {@link BeaconMenuOpener#open} re-checks that it is in
 * range and actually holds a beacon. {@link #FIND_ANY} lets the key binding say "whichever one I
 * have" without the client having to scan.
 */
public record OpenBeaconPayload(int slot) implements CustomPacketPayload {

    public static final int FIND_ANY = -1;

    public static final Type<OpenBeaconPayload> TYPE = new Type<>(BPRegistryKeys.id("open_beacon"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, OpenBeaconPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenBeaconPayload::slot, OpenBeaconPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID)
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
                                ? BeaconMenuOpener.findBeacon(player)
                                : payload.slot();
                        // Only "no beacon at all" is rejected here. This used to read `slot >= 0`,
                        // which silently swallowed the negative sentinel meaning "the beacon worn as
                        // a curio" - pressing the key while wearing one did nothing whatsoever.
                        // BeaconMenuOpener.open validates the index itself, so duplicating the range
                        // check here bought nothing and cost that.
                        if (slot != BeaconMenuOpener.NONE) {
                            BeaconMenuOpener.open(player, slot);
                        }
                    }));
        }

        private Handler() {}
    }
}

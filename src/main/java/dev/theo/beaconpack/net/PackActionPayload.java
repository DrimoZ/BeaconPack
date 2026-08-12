package dev.theo.beaconpack.net;

import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.menu.BeaconPackMenu;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * A configuration change made in the pack screen.
 *
 * <p>Deliberately not {@code clickMenuButton}: vanilla's container-button packet serialises the
 * button id as a single byte, so anything packed into a wider int silently arrives truncated - and
 * a truncated id decodes as a different action rather than as an error.
 */
public record PackActionPayload(int action, int slot, int value) implements CustomPacketPayload {

    public static final Type<PackActionPayload> TYPE = new Type<>(BPRegistryKeys.id("pack_action"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PackActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PackActionPayload::action,
                    ByteBufCodecs.VAR_INT, PackActionPayload::slot,
                    ByteBufCodecs.VAR_INT, PackActionPayload::value,
                    PackActionPayload::new);

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
                        // Routed through the open menu, so it can only ever touch the pack the
                        // player already has open.
                        if (context.player() instanceof ServerPlayer player
                                && player.containerMenu instanceof BeaconPackMenu menu) {
                            menu.applyAction(payload.action(), payload.slot(), payload.value());
                        }
                    }));
        }

        private Handler() {}
    }
}

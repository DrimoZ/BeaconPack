package dev.theo.beaconpack.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.core.AugmentDef;
import dev.theo.beaconpack.core.AugmentInstance;
import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.item.AugmentItem;
import dev.theo.beaconpack.net.OpenPackPayload;
import dev.theo.beaconpack.registry.BPItems;
import dev.theo.beaconpack.registry.BPMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class BPClientEvents {

    public static final KeyMapping OPEN_PACK = new KeyMapping(
            "key.beaconpack.open_pack",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.inventory");

    @EventBusSubscriber(modid = BeaconPack.MOD_ID, bus = EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(BPMenus.BEACON_PACK.get(), BeaconPackScreen::new);
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_PACK);
        }

        /**
         * Tints the shared augment texture from its registry entry, which is what lets a
         * datapack-added augment look distinct without shipping a model or a texture.
         */
        @SubscribeEvent
        public static void registerColours(RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> {
                AugmentInstance instance = AugmentItem.instanceOf(stack);
                if (instance == null || Minecraft.getInstance().level == null) {
                    return 0xFFFFFF;
                }
                AugmentDef def = Minecraft.getInstance().level.registryAccess()
                        .registryOrThrow(BPRegistryKeys.AUGMENT).get(instance.type());
                return def == null ? 0xFFFFFF : def.color();
            }, BPItems.AUGMENT.get());
        }

        private ModBus() {}
    }

    @EventBusSubscriber(modid = BeaconPack.MOD_ID, bus = EventBusSubscriber.Bus.GAME,
            value = Dist.CLIENT)
    public static final class GameBus {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_PACK.consumeClick()) {
                // No slot index: the server finds the pack itself, so a crafted packet cannot
                // point the menu at an arbitrary stack.
                PacketDistributor.sendToServer(new OpenPackPayload(OpenPackPayload.FIND_ANY));
            }
        }

        private GameBus() {}
    }

    private BPClientEvents() {}
}

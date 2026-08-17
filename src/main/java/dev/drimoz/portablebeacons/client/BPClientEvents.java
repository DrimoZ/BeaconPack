package dev.drimoz.portablebeacons.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.net.OpenPackPayload;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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

    /**
     * Opens the active pack without having to hold it.
     *
     * <p>Its own category rather than the vanilla "Inventory" one: filed there it sat among twenty
     * vanilla binds and was effectively impossible to find, which reads as the bind not existing.
     */
    public static final KeyMapping OPEN_PACK = new KeyMapping(
            "key.beaconpack.open_pack",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.beaconpack");

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(BPMenus.BEACON_PACK.get(), PortableBeaconScreen::new);
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
                AugmentDef def = definitionOf(stack);
                // The alpha channel is honoured when tinting, so a plain 0xRRGGBB from the JSON
                // renders the item fully transparent. Force it opaque.
                return 0xFF000000 | (def == null ? 0xFFFFFF : def.color());
            }, BPItems.AUGMENT.get());
        }

        /**
         * Exposes the augment's model_data as a model predicate, so each type gets its own glyph
         * without needing a component on the stack - which would mean repeating the value in every
         * recipe result as well as in the registry entry.
         */
        @SubscribeEvent
        public static void registerModelProperties(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(
                    BPItems.AUGMENT.get(),
                    BPRegistryKeys.id("augment_type"),
                    (stack, level, entity, seed) -> {
                        AugmentDef def = definitionOf(stack);
                        return def == null ? 0.0F : def.modelData();
                    }));
        }

        private static AugmentDef definitionOf(net.minecraft.world.item.ItemStack stack) {
            AugmentInstance instance = AugmentItem.instanceOf(stack);
            if (instance == null || Minecraft.getInstance().level == null) {
                return null;
            }
            return Minecraft.getInstance().level.registryAccess()
                    .lookupOrThrow(BPRegistryKeys.AUGMENT).get(instance.type());
        }

        private ModBus() {}
    }

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID,
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

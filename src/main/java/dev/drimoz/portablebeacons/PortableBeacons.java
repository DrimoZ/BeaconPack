package dev.drimoz.portablebeacons;

import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.gametest.BPTestFunctions;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPCreativeTabs;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(PortableBeacons.MOD_ID)
public class PortableBeacons {

    public static final String MOD_ID = BPRegistryKeys.MOD_ID;

    public PortableBeacons(IEventBus modEventBus, ModContainer modContainer) {
        BPComponents.REGISTRAR.register(modEventBus);
        BPItems.ITEMS.register(modEventBus);
        BPCreativeTabs.TABS.register(modEventBus);
        BPMenus.MENUS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, BPConfig.SPEC);

        // Unconditional, and harmless outside a test run: it only puts functions in a registry that
        // nothing reads unless a test_instance names them.
        BPTestFunctions.register();
    }
}
